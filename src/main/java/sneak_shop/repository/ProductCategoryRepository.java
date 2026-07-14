package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.ProductCategoryEntity;
import sneak_shop.enums.CategoryStatus;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Integer> {
    Optional<ProductCategoryEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<ProductCategoryEntity> findByParentIsNullAndStatus(CategoryStatus status);
    List<ProductCategoryEntity> findByParentIdAndStatus(Integer parentId, CategoryStatus status);
    List<ProductCategoryEntity> findByStatusOrderBySortOrderAsc(CategoryStatus status);

    // 3 repo methods riêng cho 3 cấp category
    @org.springframework.data.jpa.repository.Query(
        "SELECT c FROM ProductCategoryEntity c WHERE c.slug = :slug AND c.parent IS NULL")
    Optional<ProductCategoryEntity> findMainCategoryBySlug(
        @org.springframework.data.repository.query.Param("slug") String slug);

    @org.springframework.data.jpa.repository.Query(
        "SELECT c FROM ProductCategoryEntity c WHERE c.slug = :slug AND c.parent.id = :mainId")
    Optional<ProductCategoryEntity> findParentCategoryUnderMain(
        @org.springframework.data.repository.query.Param("mainId") Integer mainId,
        @org.springframework.data.repository.query.Param("slug") String slug);

    @org.springframework.data.jpa.repository.Query(
        "SELECT c FROM ProductCategoryEntity c WHERE c.slug = :slug AND c.parent.id = :parentId")
    Optional<ProductCategoryEntity> findChildCategoryUnderParent(
        @org.springframework.data.repository.query.Param("parentId") Integer parentId,
        @org.springframework.data.repository.query.Param("slug") String slug);
}
