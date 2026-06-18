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
import java.util.List;
import java.util.Objects;

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
                .stream().filter(c -> !c.isDeleted()).map(CategoryResponse::from).toList();
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
            throw new AppException(ErrorCode.CONFLICT, "Slug da ton tai");
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
            throw new AppException(ErrorCode.CONFLICT, "Slug da ton tai");
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
        if (!Objects.equals(oldParentId, parentKey(parent))) {
            renumberAndSave(loadSiblings(oldParentId, id));
        }
        return CategoryResponse.from(entity);
    }

    public long countProducts(Integer id) {
        return mappingRepository.countByCategoryId(id);
    }

    public void delete(Integer id, Integer moveProductsToId) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc khong ton tai"));

        if (moveProductsToId != null) {
            if (moveProductsToId.equals(id)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Danh muc dich khong the la chinh danh muc dang xoa");
            }
            ProductCategoryEntity target = categoryRepository.findById(moveProductsToId)
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc dich khong ton tai"));
            for (var mapping : mappingRepository.findByCategoryId(id)) {
                if (mappingRepository.existsByProductIdAndCategoryId(mapping.getProduct().getId(), moveProductsToId)) {
                    mappingRepository.delete(mapping);
                } else {
                    mapping.setCategory(target);
                    mappingRepository.save(mapping);
                }
            }
        } else {
            mappingRepository.deleteByCategoryId(id);
        }

        entity.setDeleted(true);
        categoryRepository.save(entity);

        List<BannerEntity> banners = bannerRepository.findAllByCategoryIdOrderBySortOrderAscIdDesc(id);
        if (banners != null && !banners.isEmpty()) {
            bannerRepository.deleteAll(banners);
        }
    }

    public void restore(Integer id) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .filter(ProductCategoryEntity::isDeleted)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh muc khong ton tai hoac chua bi xoa"));
        entity.setDeleted(false);
        categoryRepository.save(entity);
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
