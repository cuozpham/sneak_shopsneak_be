package sneak_shop.service;

import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ReviewRequest;
import sneak_shop.dto.request.ShopReplyRequest;
import sneak_shop.dto.response.ReviewResponse;
import sneak_shop.entity.ProductImageEntity;

public interface ReviewService {
    PageResponse<ReviewResponse> getByProduct(Integer productId, int page, int size);
    PageResponse<ReviewResponse> getMyReviews(Integer userId, int page, int size);
    ReviewResponse getById(Integer reviewId);
    ReviewResponse create(Integer userId, ReviewRequest req);
    ReviewResponse update(Integer userId, Integer reviewId, ReviewRequest req);
    ReviewResponse shopReply(Integer reviewId, ShopReplyRequest req);
    ReviewResponse shopReplyByAdmin(sneak_shop.security.UserContext ctx, Integer reviewId, String content);
    ReviewResponse deleteShopReplyByAdmin(sneak_shop.security.UserContext ctx, Integer reviewId);
    ProductImageEntity saveReviewImage(Integer productId, String imageUrl);
    ReviewResponse customerReply(Integer userId, Integer reviewId, String reply);
}
