package sneak_shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.ShippingFeeConfigEntity;

import java.time.LocalDate;
import java.util.Optional;

public interface ShippingFeeConfigRepository extends JpaRepository<ShippingFeeConfigEntity, Integer> {

    Page<ShippingFeeConfigEntity> findAllByOrderByEffectiveMonthDesc(Pageable pageable);

    Optional<ShippingFeeConfigEntity> findByEffectiveMonth(LocalDate effectiveMonth);

    Optional<ShippingFeeConfigEntity> findFirstByEffectiveMonthLessThanEqualOrderByEffectiveMonthDesc(
            LocalDate effectiveMonth
    );
}
