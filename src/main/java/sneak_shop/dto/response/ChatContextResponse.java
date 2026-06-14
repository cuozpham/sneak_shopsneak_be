package sneak_shop.dto.response;

import java.math.BigDecimal;

public record ChatContextResponse(
        String conversationId,
        ProductPreview product
) {
    public record ProductPreview(
            Integer productId,
            String name,
            String slug,
            String imageUrl,
            BigDecimal price,
            BigDecimal discountPrice
    ) {}
}
