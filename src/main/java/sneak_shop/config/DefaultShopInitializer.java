package sneak_shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sneak_shop.entity.ProductShopEntity;
import sneak_shop.repository.ProductShopRepository;

@Configuration
@Slf4j
public class DefaultShopInitializer {

    private static final String DEFAULT_SHOP_NAME = "sneak";

    @Bean
    CommandLineRunner seedDefaultShop(ProductShopRepository shopRepository) {
        return args -> {
            try {
                shopRepository.findByNameIgnoreCase(DEFAULT_SHOP_NAME)
                        .orElseGet(() -> shopRepository.save(ProductShopEntity.builder()
                                .name(DEFAULT_SHOP_NAME)
                                .build()));
            } catch (Exception ex) {
                log.warn("Skipping default shop seed during startup: {}", ex.getMessage(), ex);
            }
        };
    }
}
