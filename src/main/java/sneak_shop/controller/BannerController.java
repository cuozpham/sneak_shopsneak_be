package sneak_shop.controller;

import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.entity.BannerEntity;
import sneak_shop.entity.ProductCategoryEntity;
import sneak_shop.repository.BannerRepository;
import sneak_shop.repository.ProductCategoryRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerRepository bannerRepository;
    private final ProductCategoryRepository categoryRepository;

    public BannerController(BannerRepository bannerRepository, ProductCategoryRepository categoryRepository) {
        this.bannerRepository = bannerRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ApiResponse<List<BannerEntity>> getActive(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "categorySlug", required = false) String categorySlug) {

        Integer targetCategoryId = categoryId;
        if (targetCategoryId == null && categorySlug != null && !categorySlug.isBlank()) {
            targetCategoryId = categoryRepository.findBySlug(categorySlug.trim())
                    .map(ProductCategoryEntity::getId)
                    .orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (targetCategoryId != null) {
            List<BannerEntity> catBanners = bannerRepository.findActiveByCategoryId(targetCategoryId, now);
            if (!catBanners.isEmpty()) {
                return ApiResponse.ok(catBanners);
            }
        }
        return ApiResponse.ok(bannerRepository.findActiveDefault(now));
    }
}
