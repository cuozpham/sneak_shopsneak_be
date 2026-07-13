package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {
    Optional<ProductEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            ORDER BY p.created_at DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchNewest(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("hasCategory") int hasCategory,
            @Param("categoryIds") Collection<Integer> categoryIds,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            ORDER BY p.price ASC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchPriceAsc(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("hasCategory") int hasCategory,
            @Param("categoryIds") Collection<Integer> categoryIds,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            ORDER BY p.price DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchPriceDesc(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("hasCategory") int hasCategory,
            @Param("categoryIds") Collection<Integer> categoryIds,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    @Query(value = """
            SELECT p.* FROM products p
            WHERE (:deleted IS NULL OR p.is_deleted = :deleted)
              AND (:status IS NULL OR p.status = :status)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.created_at DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(p.id) FROM products p
            WHERE (:deleted IS NULL OR p.is_deleted = :deleted)
              AND (:status IS NULL OR p.status = :status)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            nativeQuery = true)
    Page<ProductEntity> adminSearch(
            @Param("deleted") Boolean deleted,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE ProductEntity p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int deductStock(@Param("id") Integer id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.stockQuantity = p.stockQuantity + :qty WHERE p.id = :id")
    void addStock(@Param("id") Integer id, @Param("qty") int qty);

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            ORDER BY (SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.product_id = p.id) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchSortBySold(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("hasCategory") int hasCategory,
            @Param("categoryIds") Collection<Integer> categoryIds,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            ORDER BY (SELECT COALESCE(AVG(r.rating), 0) FROM reviews r WHERE r.product_id = p.id) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND p.status != 'inactive'
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:hasCategory = 0 OR m.category_id IN :categoryIds)
              AND (:minRating IS NULL OR COALESCE(p.rating_average, 0) >= :minRating)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchSortByRating(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("hasCategory") int hasCategory,
            @Param("categoryIds") Collection<Integer> categoryIds,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.is_deleted = false
              AND p.status = 'active'
              AND p.is_featured = 1
              AND p.price > 0
            ORDER BY COALESCE(p.featured_order, 999999) ASC, p.created_at DESC, p.id DESC
            """,
            nativeQuery = true)
    List<ProductEntity> findPinnedFeatured();

    long countByFeaturedTrue();

    @Modifying
    @Query(value = """
            UPDATE products
            SET featured_order = featured_order + 1
            WHERE is_featured = 1
              AND is_deleted = false
              AND featured_order IS NOT NULL
              AND featured_order >= :fromOrder
              AND (:shopId IS NULL OR shop_id = :shopId)
              AND id <> :excludeId
            """,
            nativeQuery = true)
    int shiftFeaturedOrderDown(@Param("fromOrder") Integer fromOrder,
                               @Param("shopId") Integer shopId,
                               @Param("excludeId") Integer excludeId);

    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.is_deleted = false
              AND p.status = 'active'
              AND p.price > 0
              AND p.id NOT IN (:excludedIds)
            ORDER BY (
                SELECT COALESCE(SUM(oi.quantity), 0)
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE oi.product_id = p.id
                  AND o.created_at >= :since
            ) DESC,
            CASE WHEN COALESCE(p.review_count, 0) >= :minReviews
                 THEN COALESCE(p.rating_average, 0)
                 ELSE 0 END DESC,
            p.created_at DESC,
            p.id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<ProductEntity> findAutoFeaturedCandidates(
            @Param("since") Instant since,
            @Param("minReviews") int minReviews,
            @Param("excludedIds") Collection<Integer> excludedIds,
            @Param("limit") int limit
    );
}
