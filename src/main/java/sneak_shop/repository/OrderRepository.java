package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sneak_shop.entity.OrderEntity;
import sneak_shop.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    Optional<OrderEntity> findByOrderCode(String orderCode);
    Optional<OrderEntity> findTopByUserIdOrderByCreatedAtDesc(Integer userId);
    boolean existsByAddressId(Integer addressId);
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<OrderEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, OrderStatus status, Pageable pageable);
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
            SELECT o.* FROM orders o
            LEFT JOIN users u ON u.id = o.user_id
            WHERE (:status IS NULL OR o.status = :status)
              AND (LOWER(COALESCE(o.order_code, '')) LIKE :kw OR
                   LOWER(COALESCE(o.recipient_name, '')) LIKE :kw OR
                   LOWER(COALESCE(u.email, '')) LIKE :kw OR
                   COALESCE(u.phone_number, '') LIKE :kw OR
                   COALESCE(o.recipient_phone, '') LIKE :kw)
            ORDER BY o.created_at DESC, o.id DESC
            """,
            countQuery = """
            SELECT COUNT(o.id) FROM orders o
            LEFT JOIN users u ON u.id = o.user_id
            WHERE (:status IS NULL OR o.status = :status)
              AND (LOWER(COALESCE(o.order_code, '')) LIKE :kw OR
                   LOWER(COALESCE(o.recipient_name, '')) LIKE :kw OR
                   LOWER(COALESCE(u.email, '')) LIKE :kw OR
                   COALESCE(u.phone_number, '') LIKE :kw OR
                   COALESCE(o.recipient_phone, '') LIKE :kw)
            """,
            nativeQuery = true)
    Page<OrderEntity> searchByKeyword(@Param("status") String status,
                                      @Param("kw") String kw,
                                      Pageable pageable);
    Long countByUserId(Integer userId);
    boolean existsByOrderCode(String orderCode);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to AND o.status <> :cancelled")
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("cancelled") OrderStatus cancelled);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to")
    Long countOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.user.id = :userId AND o.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") OrderStatus status);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM OrderEntity o
            WHERE o.user.id = :userId
              AND o.status = :status
            """)
    BigDecimal sumUserSpent(
            @Param("userId") Integer userId,
            @Param("status") OrderStatus status
    );

    List<OrderEntity> findByCreatedAtGreaterThanEqual(Instant from);
}
