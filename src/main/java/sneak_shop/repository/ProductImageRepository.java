package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.ProductImageEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Integer> {
    List<ProductImageEntity> findByProductIdOrderBySortOrderAsc(Integer productId);
    List<ProductImageEntity> findByProductIdAndTypeNotOrderBySortOrderAsc(Integer productId, String type);
    List<ProductImageEntity> findByProductIdInAndTypeNotOrderByProductIdAscSortOrderAsc(Collection<Integer> productIds, String type);
    Optional<ProductImageEntity> findFirstByProductIdAndType(Integer productId, String type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProductImageEntity i WHERE i.product.id = :productId AND (i.type IS NULL OR i.type <> 'review')")
    void deleteByProductId(@Param("productId") Integer productId);
}
