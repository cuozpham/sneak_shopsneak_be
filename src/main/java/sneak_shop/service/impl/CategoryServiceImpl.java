package sneak_shop.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.dto.request.CategoryRequest;
import sneak_shop.dto.response.CategoryResponse;
import sneak_shop.entity.ProductCategoryEntity;
import sneak_shop.enums.CategoryStatus;
import sneak_shop.repository.ProductCategoryMappingRepository;
import sneak_shop.repository.ProductCategoryRepository;
import sneak_shop.service.CategoryService;

import sneak_shop.entity.BannerEntity;
import sneak_shop.repository.BannerRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

@Transactional
@Service
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductCategoryMappingRepository mappingRepository;
    private final BannerRepository bannerRepository;

    public CategoryServiceImpl(ProductCategoryRepository categoryRepository,
                               ProductCategoryMappingRepository mappingRepository,
                               BannerRepository bannerRepository) {
        this.categoryRepository = categoryRepository;
        this.mappingRepository = mappingRepository;
        this.bannerRepository = bannerRepository;
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findByStatusOrderBySortOrderAsc(CategoryStatus.active)
                .stream().filter(c -> !c.isDeleted())
                .map(c -> CategoryResponse.from(c, mappingRepository.countByCategoryId(c.getId())))
                .toList();
    }

    public List<CategoryResponse> adminGetAll() {
        return adminGetAll(null);
    }

    public List<CategoryResponse> adminGetAll(Boolean deleted) {
        return categoryRepository.findAll().stream()
                .filter(c -> deleted == null || c.isDeleted() == deleted)
                .sorted(Comparator.comparingInt((ProductCategoryEntity c) -> c.getSortOrder() != null ? c.getSortOrder() : 0))
                .map(CategoryResponse::from).toList();
    }

    public List<CategoryResponse> getRoots() {
        return categoryRepository.findByParentIsNullAndStatus(CategoryStatus.active)
                .stream().filter(c -> !c.isDeleted()).map(CategoryResponse::from).toList();
    }

    public CategoryResponse getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc khong ton tai"));
    }

    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsBySlug(req.slug())) {
            throw new AppException(ErrorCode.CONFLICT, "Đã có danh mục trùng tên này, vui lòng lấy tên danh mục khác");
        }
        ProductCategoryEntity parent = resolveParent(req.parentId(), null);

        ProductCategoryEntity entity = ProductCategoryEntity.builder()
                .name(req.name()).slug(req.slug()).description(req.description())
                .imageUrl(req.imageUrl()).parent(parent)
                .status(req.status() != null ? req.status() : CategoryStatus.active)
                .build();
        entity = categoryRepository.save(entity);
        reposition(entity, parent, req.sortOrder(), null);
        return CategoryResponse.from(entity);
    }

    public CategoryResponse update(Integer id, CategoryRequest req) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc khong ton tai"));
        Integer oldParentId = parentKey(entity);

        if (!entity.getSlug().equals(req.slug()) && categoryRepository.existsBySlug(req.slug())) {
            throw new AppException(ErrorCode.CONFLICT, "Đã có danh mục trùng tên này, vui lòng lấy tên danh mục khác");
        }

        ProductCategoryEntity parent = resolveParent(req.parentId(), id);

        entity.setName(req.name());
        entity.setSlug(req.slug());
        entity.setDescription(req.description());
        entity.setImageUrl(req.imageUrl());
        entity.setParent(parent);
        if (req.status() != null) entity.setStatus(req.status());
        entity = categoryRepository.save(entity);
        reposition(entity, parent, req.sortOrder(), id);
        Integer newParentId = parent != null ? parent.getId() : null;
        if (!Objects.equals(oldParentId, newParentId)) {
            renumberAndSave(loadSiblings(oldParentId, id));
        }
        return CategoryResponse.from(entity);
    }

    public long countProducts(Integer id) {
        return collectIdsWithDescendants(id).stream()
                .mapToLong(mappingRepository::countByCategoryId)
                .sum();
    }

    public void delete(Integer id) {
        categoryRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc khong ton tai"));

        List<Integer> ids = collectIdsWithDescendants(id);
        List<ProductCategoryEntity> entities = categoryRepository.findAllById(ids);
        for (ProductCategoryEntity entity : entities) {
            entity.setDeleted(true);

            List<BannerEntity> banners = bannerRepository.findAllByCategoryIdOrderBySortOrderAscIdDesc(entity.getId());
            if (banners != null && !banners.isEmpty()) {
                bannerRepository.deleteAll(banners);
            }
        }
        categoryRepository.saveAll(entities);
    }

    public CategoryResponse restore(Integer id) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .filter(ProductCategoryEntity::isDeleted)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc khong ton tai trong thung rac"));

        if (entity.getParent() != null && entity.getParent().isDeleted()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Bạn cần khôi phục danh mục cha \"" + entity.getParent().getName() + "\" trước");
        }

        List<ProductCategoryEntity> all = categoryRepository.findAll();
        List<ProductCategoryEntity> toRestore = collectDeletedWithDescendants(id, all);
        for (ProductCategoryEntity cat : toRestore) {
            cat.setDeleted(false);
        }
        categoryRepository.saveAll(toRestore);

        reposition(entity, entity.getParent(), null, id);
        return CategoryResponse.from(entity);
    }

    private List<ProductCategoryEntity> collectDeletedWithDescendants(Integer rootId, List<ProductCategoryEntity> all) {
        Map<Integer, ProductCategoryEntity> byId = new HashMap<>();
        Map<Integer, List<Integer>> childrenByParentId = new HashMap<>();
        for (ProductCategoryEntity c : all) {
            byId.put(c.getId(), c);
            if (c.getParent() != null) {
                childrenByParentId.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c.getId());
            }
        }

        List<ProductCategoryEntity> result = new ArrayList<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(rootId);

        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();
            ProductCategoryEntity current = byId.get(currentId);
            if (current == null || !current.isDeleted()) continue;
            result.add(current);
            queue.addAll(childrenByParentId.getOrDefault(currentId, List.of()));
        }
        return result;
    }

    private List<Integer> collectIdsWithDescendants(Integer rootId) {
        List<ProductCategoryEntity> active = categoryRepository.findAll().stream()
                .filter(c -> !c.isDeleted())
                .toList();
        List<Integer> ids = new ArrayList<>();
        ids.add(rootId);
        collectDescendants(rootId, active, ids);
        return ids;
    }

    private void collectDescendants(Integer parentId, List<ProductCategoryEntity> all, List<Integer> target) {
        for (ProductCategoryEntity category : all) {
            if (category.getParent() != null && parentId.equals(category.getParent().getId())
                    && !target.contains(category.getId())) {
                target.add(category.getId());
                collectDescendants(category.getId(), all, target);
            }
        }
    }

    private ProductCategoryEntity resolveParent(Integer parentId, Integer selfId) {
        if (parentId == null) return null;
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh muc cha khong the la chinh no");
        }
        ProductCategoryEntity parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Parent category khong ton tai"));
        if (selfId != null && isDescendant(parent, selfId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the chon danh muc con lam danh muc cha");
        }
        return parent;
    }

    private boolean isDescendant(ProductCategoryEntity candidateParent, Integer ancestorId) {
        ProductCategoryEntity current = candidateParent;
        while (current != null) {
            if (Objects.equals(current.getId(), ancestorId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void reposition(ProductCategoryEntity entity,
                            ProductCategoryEntity parent,
                            Integer requestedSortOrder,
                            Integer excludeId) {
        Integer parentId = parent != null ? parent.getId() : null;
        List<ProductCategoryEntity> siblings = loadSiblings(parentId, excludeId == null ? entity.getId() : excludeId);
        int targetIndex = resolveTargetIndex(requestedSortOrder, siblings.size());
        siblings.add(targetIndex - 1, entity);
        renumberAndSave(siblings);
    }

    private List<ProductCategoryEntity> loadSiblings(Integer parentId, Integer excludeId) {
        List<ProductCategoryEntity> siblings = new ArrayList<>(categoryRepository.findAll().stream()
                .filter(c -> !c.isDeleted())
                .filter(c -> Objects.equals(parentKey(c), parentId))
                .filter(c -> excludeId == null || !Objects.equals(c.getId(), excludeId))
                .sorted(Comparator
                        .comparingInt((ProductCategoryEntity c) -> c.getSortOrder() != null ? c.getSortOrder() : Integer.MAX_VALUE)
                        .thenComparing(ProductCategoryEntity::getId))
                .toList());
        return siblings;
    }

    private Integer parentKey(ProductCategoryEntity category) {
        return category.getParent() != null ? category.getParent().getId() : null;
    }

    private int resolveTargetIndex(Integer requestedSortOrder, int siblingCount) {
        if (requestedSortOrder == null || requestedSortOrder <= 0) {
            return siblingCount + 1;
        }
        return Math.min(requestedSortOrder, siblingCount + 1);
    }

    private void renumberAndSave(List<ProductCategoryEntity> categories) {
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setSortOrder(i + 1);
        }
        categoryRepository.saveAll(categories);
    }
}
