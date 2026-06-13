package sneak_shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShippingFeeSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ShippingFeeSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
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
                    INSERT INTO shipping_fee_configs (effective_month, fee)
                    SELECT DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), 30000.00
                    WHERE NOT EXISTS (SELECT 1 FROM shipping_fee_configs)
                    """);
        } catch (Exception exception) {
            log.warn(
                    "Shipping fee table initialization was deferred because the database is not ready: {}",
                    exception.getMessage()
            );
        }
    }
}
