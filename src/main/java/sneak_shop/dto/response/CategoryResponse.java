package sneak_shop.dto.response;

import sneak_shop.entity.ProductCategoryEntity;
import sneak_shop.enums.CategoryStatus;

public record CategoryResponse(
        Integer id,
        String name,
        String slug,
        String description,
        String imageUrl,
        Integer parentId,
        String parentName,
        Integer sortOrder,
        CategoryStatus status,
        boolean deleted,
        long productCount
) {
    public static CategoryResponse from(ProductCategoryEntity e) {
        return from(e, 0);
    }

    public static CategoryResponse from(ProductCategoryEntity e, long productCount) {
        return new CategoryResponse(e.getId(), e.getName(), e.getSlug(), e.getDescription(),
                e.getImageUrl(),
                e.getParent() != null ? e.getParent().getId() : null,
                e.getParent() != null ? e.getParent().getName() : null,
                e.getSortOrder(), e.getStatus(), e.isDeleted(), productCount);
    }
}
