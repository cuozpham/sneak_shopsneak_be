package sneak_shop.dto.response;

import sneak_shop.entity.NotificationEntity;

import java.time.Instant;

public record NotificationResponse(
        Integer id,
        String title,
        String body,
        String imageUrl,
        String type,
        String orderCode,
        Integer productId,
        String productSlug,
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(NotificationEntity e) {
        return from(e, e != null && e.getOrder() != null ? e.getOrder().getOrderCode() : null);
    }

    public static NotificationResponse from(NotificationEntity e, String orderCode) {
        Integer productId = e.getProduct() != null ? e.getProduct().getId() : null;
        String productSlug = e.getProduct() != null ? e.getProduct().getSlug() : null;
        return new NotificationResponse(e.getId(), e.getTitle(), e.getBody(),
                e.getImageUrl(), e.getType(),
                orderCode,
                productId, productSlug,
                e.getIsRead(), e.getCreatedAt());
    }
}
