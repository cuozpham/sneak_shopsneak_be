package sneak_shop.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sneak_shop.entity.ProductEntity;
import sneak_shop.entity.ProductShopEntity;
import sneak_shop.repository.ProductRepository;
import sneak_shop.repository.ProductShopRepository;

import java.util.List;

@Configuration
public class DefaultShopInitializer {

    private static final String DEFAULT_SHOP_NAME = "sneak";

    @Bean
    CommandLineRunner seedDefaultShop(ProductShopRepository shopRepository, ProductRepository productRepository) {
        return args -> {
            ProductShopEntity shop = shopRepository.findByNameIgnoreCase(DEFAULT_SHOP_NAME)
                    .orElseGet(() -> shopRepository.save(ProductShopEntity.builder()
                            .name(DEFAULT_SHOP_NAME)
                            .build()));

            List<ProductEntity> orphanProducts = productRepository.findByShopIsNullAndDeletedFalse();
            if (!orphanProducts.isEmpty()) {
                orphanProducts.forEach(product -> product.setShop(shop));
                productRepository.saveAll(orphanProducts);
            }
        };
    }
}
