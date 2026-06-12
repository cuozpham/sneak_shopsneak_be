package sneak_shop.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sneak_shop.entity.ProductShopEntity;
import sneak_shop.repository.ProductShopRepository;

@Configuration
public class DefaultShopInitializer {

    private static final String DEFAULT_SHOP_NAME = "sneak";

    @Bean
    CommandLineRunner seedDefaultShop(ProductShopRepository shopRepository) {
        return args -> {
            shopRepository.findByNameIgnoreCase(DEFAULT_SHOP_NAME)
                    .orElseGet(() -> shopRepository.save(ProductShopEntity.builder()
                            .name(DEFAULT_SHOP_NAME)
                            .build()));
        };
    }
}
