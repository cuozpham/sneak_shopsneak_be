package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import sneak_shop.entity.ProductEntity;
import sneak_shop.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {
    Optional<ProductEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            ORDER BY p.created_at DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchNewest(
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            ORDER BY p.price ASC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchPriceAsc(
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            ORDER BY p.price DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchPriceDesc(
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
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
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            ORDER BY (SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.product_id = p.id) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchSortBySold(
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            ORDER BY (SELECT COALESCE(AVG(r.rating), 0) FROM reviews r WHERE r.product_id = p.id) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            LEFT JOIN product_category_mappings m ON m.product_id = p.id
            WHERE p.is_deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR m.category_id = :categoryId)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchSortByRating(
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );
}
