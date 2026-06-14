package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductShopEntity;

import java.util.Optional;

public interface ProductShopRepository extends JpaRepository<ProductShopEntity, Integer> {

    @Query(value = """
            SELECT s.* FROM product_shops s
            WHERE (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY s.created_at DESC, s.id DESC
            """,
            countQuery = """
            SELECT COUNT(s.id) FROM product_shops s
            WHERE (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            nativeQuery = true)
    Page<ProductShopEntity> search(@Param("keyword") String keyword, Pageable pageable);

    Optional<ProductShopEntity> findByNameIgnoreCase(String name);
}
