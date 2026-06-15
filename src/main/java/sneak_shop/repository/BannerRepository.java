package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.BannerEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<BannerEntity, Integer> {

    @Query("""
            SELECT b
            FROM BannerEntity b
            WHERE b.isActive = true
              AND (b.startDate IS NULL OR b.startDate <= :now)
              AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<BannerEntity> findActiveForDisplay(@Param("now") LocalDateTime now);

    @Query("""
            SELECT b
            FROM BannerEntity b
            WHERE b.isActive = true
              AND b.categoryId IS NULL
              AND (b.startDate IS NULL OR b.startDate <= :now)
              AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<BannerEntity> findActiveDefault(@Param("now") LocalDateTime now);

    @Query("""
            SELECT b
            FROM BannerEntity b
            WHERE b.isActive = true
              AND b.categoryId = :categoryId
              AND (b.startDate IS NULL OR b.startDate <= :now)
              AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.sortOrder ASC, b.id DESC
            """)
    List<BannerEntity> findActiveByCategoryId(@Param("categoryId") Integer categoryId, @Param("now") LocalDateTime now);

    long countByCategoryId(Integer categoryId);
    long countByCategoryIdIsNull();

    @Query("SELECT MAX(b.sortOrder) FROM BannerEntity b WHERE b.categoryId = :categoryId")
    Integer findMaxSortOrder(@Param("categoryId") Integer categoryId);

    @Query("SELECT MAX(b.sortOrder) FROM BannerEntity b WHERE b.categoryId IS NULL")
    Integer findMaxSortOrderIsNull();

    List<BannerEntity> findAllByCategoryIdOrderBySortOrderAscIdDesc(Integer categoryId);
    List<BannerEntity> findAllByCategoryIdIsNullOrderBySortOrderAscIdDesc();

    List<BannerEntity> findAllByOrderBySortOrderAscIdDesc();
}
