package sneak_shop.dto.request;

import java.time.LocalDateTime;

public record BannerRequest(
        String title,
        String imageUrl,
        String linkUrl,
        String position,
        String objectPosition,
        Boolean isActive,
        Integer sortOrder,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer categoryId
) {}
