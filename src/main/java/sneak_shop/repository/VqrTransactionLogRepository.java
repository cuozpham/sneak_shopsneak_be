package sneak_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sneak_shop.entity.VqrTransactionLog;

public interface VqrTransactionLogRepository extends JpaRepository<VqrTransactionLog, Integer> {
}
