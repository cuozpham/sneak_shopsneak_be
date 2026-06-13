package sneak_shop.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ShippingFeeTableManager {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public ShippingFeeTableManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureInitialized() {
        if (initialized.get()) return;

        synchronized (initialized) {
            if (initialized.get()) return;

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS shipping_fee_configs (
                        id INT NOT NULL AUTO_INCREMENT,
                        effective_month DATE NOT NULL,
                        fee DECIMAL(15, 2) NOT NULL DEFAULT 30000.00,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                            ON UPDATE CURRENT_TIMESTAMP(6),
                        PRIMARY KEY (id),
                        CONSTRAINT uk_shipping_fee_configs_effective_month UNIQUE (effective_month)
                    )
                    """);

            jdbcTemplate.update("""
                    INSERT INTO shipping_fee_configs (
                        effective_month, fee, created_at, updated_at
                    )
                    SELECT DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), 30000.00, NOW(6), NOW(6)
                    WHERE NOT EXISTS (SELECT 1 FROM shipping_fee_configs)
                    """);

            initialized.set(true);
        }
    }
}
