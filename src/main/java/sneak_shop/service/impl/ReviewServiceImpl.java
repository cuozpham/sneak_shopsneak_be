package sneak_shop.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ReviewRequest;
import sneak_shop.dto.request.ShopReplyRequest;
import sneak_shop.dto.response.ReviewResponse;
import sneak_shop.entity.*;
import sneak_shop.enums.OrderStatus;
import sneak_shop.repository.*;
import sneak_shop.service.ReviewService;
import sneak_shop.service.NotificationService;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final String EDIT_MARKER = "\u0001EDITED_ONCE\u0001";
    private static final Duration REVIEW_WINDOW = Duration.ofDays(7);

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductShopRepository shopRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final NotificationService notificationService;

    public ReviewServiceImpl(ReviewRepository reviewRepository, UserRepository userRepository,
                             OrderItemRepository orderItemRepository, ProductRepository productRepository,
                             ProductShopRepository shopRepository,
                             ProductImageRepository productImageRepository,
                             ReviewImageRepository reviewImageRepository,
                             NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.productImageRepository = productImageRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAll(int page, int size, Integer rating, LocalDate fromDate, LocalDate toDate) {
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant fromInstant = fromDate != null ? fromDate.atStartOfDay(vn).toInstant() : null;
        Instant toInstant = toDate != null ? toDate.plusDays(1).atStartOfDay(vn).toInstant() : null;
        return PageResponse.from(reviewRepository.adminSearch(rating, fromInstant, toInstant,
                PageRequest.of(page, size)).map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByProduct(Integer productId, int page, int size) {
        return PageResponse.from(reviewRepository.findByProductIdAndProductDeletedFalseOrderByCreatedAtDesc(
                productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(Integer userId, int page, int size) {
        return PageResponse.from(reviewRepository.findByUserIdAndProductDeletedFalseOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)).map(ReviewResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getById(Integer reviewId) {
        return reviewRepository.findByIdAndProductDeletedFalse(reviewId)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
    }

    @Transactional
    public ReviewResponse create(Integer userId, ReviewRequest req) {
        validateReviewImages(req.productImageIds());
        if (reviewRepository.existsByOrderItemId(req.orderItemId())) {
            throw new AppException(ErrorCode.CONFLICT, "Ban da danh gia san pham nay roi");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        OrderItemEntity orderItem = orderItemRepository.findById(req.orderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Order item khong ton tai"));
        if (!orderItem.getOrder().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong duoc danh gia don hang cua nguoi khac");
        }
        if (orderItem.getOrder().getStatus() != OrderStatus.completed) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi co the danh gia don hang da hoan thanh");
        }
        Instant completedAt = orderItem.getOrder().getCompletedAt() != null
                ? orderItem.getOrder().getCompletedAt()
                : orderItem.getOrder().getUpdatedAt();
        if (completedAt == null || Instant.now().isAfter(completedAt.plus(REVIEW_WINDOW))) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Da het han danh gia 7 ngay ke tu khi don hang hoan thanh");
        }
        ProductEntity product = productRepository.findById(orderItem.getProduct().getId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        ProductShopEntity shop = resolveReviewShop(orderItem, product);
        if (shop == null || shop.getId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Don hang chua gan shop");
        }
        shop = shopRepository.findById(shop.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop khong ton tai"));

        ReviewEntity review = ReviewEntity.builder()
                .user(user)
                .orderItem(orderItem)
                .product(product)
                .shop(shop)
                .rating(req.rating())
                .comment(req.comment())
                .build();
        review = reviewRepository.save(review);

        replaceReviewImages(review, req.productImageIds());
        syncProductRatingAfterNewReview(product.getId(), req.rating());
        try {
            notificationService.notifyAdmins(
                    orderItem.getOrder(),
                    "Có đánh giá mới",
                    "Sản phẩm " + product.getName() + " vừa có một đánh giá mới.",
                    "review_new",
                    null
            );
        } catch (Exception ex) {
            log.warn("Failed to notify admins about review {}: {}", review.getId(), ex.getMessage(), ex);
        }
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse update(Integer userId, Integer reviewId, ReviewRequest req) {
        validateReviewImages(req.productImageIds());
        ReviewEntity review = reviewRepository.findByIdAndProductDeletedFalse(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong co quyen sua review nay");
        }
        if (review.getShopReply() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Da co phan hoi cua shop, khong the sua danh gia");
        }
        if (isEditedOnce(review.getComment())) {
            throw new AppException(ErrorCode.CONFLICT, "Ban chi duoc sua danh gia mot lan");
        }
        OrderEntity order = review.getOrderItem() != null ? review.getOrderItem().getOrder() : null;
        if (order == null || order.getStatus() != OrderStatus.completed) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi co the sua danh gia khi don hang da hoan thanh");
        }
        Instant completedAt = order.getCompletedAt() != null
                ? order.getCompletedAt()
                : order.getUpdatedAt();
        if (completedAt == null || Instant.now().isAfter(completedAt.plus(REVIEW_WINDOW))) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Da het han sua danh gia 7 ngay ke tu khi don hang hoan thanh");
        }

        if (req.rating() != null) review.setRating(req.rating());
        review.setComment(EDIT_MARKER + (req.comment() == null ? "" : req.comment()));
        review = reviewRepository.save(review);
        replaceReviewImages(review, req.productImageIds());
        syncProductRating(review.getProduct().getId());
        return ReviewResponse.from(review);
    }

    private ProductShopEntity resolveReviewShop(OrderItemEntity orderItem, ProductEntity product) {
        ProductShopEntity shop = orderItem.getOrder().getShop();
        if (shop != null && shop.getId() != null) {
            return shop;
        }
        if (product != null && product.getShop() != null && product.getShop().getId() != null) {
            return product.getShop();
        }
        return orderItemRepository.findByOrderId(orderItem.getOrder().getId()).stream()
                .map(OrderItemEntity::getProduct)
                .filter(p -> p != null && p.getShop() != null && p.getShop().getId() != null)
                .map(ProductEntity::getShop)
                .findFirst()
                .orElseGet(this::resolveDefaultShop);
    }

    private ProductShopEntity resolveDefaultShop() {
        return shopRepository.findByNameIgnoreCase("sneak")
                .orElseGet(() -> shopRepository.save(ProductShopEntity.builder().name("sneak").build()));
    }

    @Transactional
    public ReviewResponse shopReply(Integer reviewId, ShopReplyRequest req) {
        ReviewEntity review = reviewRepository.findByIdAndProductDeletedFalse(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        review.setShopReply(req.reply());
        review.setShopReplyAt(Instant.now());
        review = reviewRepository.save(review);
        syncProductRating(review.getProduct().getId());
        notificationService.notifyUser(
                review.getUser().getId(),
                review.getOrderItem() != null ? review.getOrderItem().getOrder() : null,
                "Shop đã phản hồi",
                "Shop vừa phản hồi đánh giá của bạn cho sản phẩm " + review.getProduct().getName() + ".",
                "review_reply",
                null
        );
        return ReviewResponse.from(review);
    }

    @Transactional
    public ProductImageEntity saveReviewImage(Integer productId, String imageUrl) {
        ProductEntity product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        return productImageRepository.save(ProductImageEntity.builder()
                .product(product)
                .imageUrl(imageUrl.trim())
                .type("review")
                .sortOrder(0)
                .build());
    }

    private void replaceReviewImages(ReviewEntity review, List<Integer> productImageIds) {
        if (productImageIds == null) return;
        reviewImageRepository.deleteByReviewId(review.getId());
        if (productImageIds.isEmpty()) {
            review.getImages().clear();
            return;
        }
        List<ReviewImageEntity> images = productImageIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .flatMap(imgId -> productImageRepository.findById(imgId).stream())
                .map(pi -> ReviewImageEntity.builder()
                        .review(review)
                        .productImage(pi)
                        .build())
                .toList();
        if (!images.isEmpty()) {
            reviewImageRepository.saveAll(images);
        }
        review.getImages().clear();
        review.getImages().addAll(reviewImageRepository.findByReviewId(review.getId()));
    }

    @Transactional
    protected void syncProductRatingAfterNewReview(Integer productId, Integer newRating) {
        if (productId == null || newRating == null) return;

        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        BigDecimal oldRatingAverage = product.getRatingAverage() != null ? product.getRatingAverage() : BigDecimal.ZERO;
        int oldReviewCount = product.getReviewCount() != null ? product.getReviewCount() : 0;

        BigDecimal totalScore = oldRatingAverage
                .multiply(BigDecimal.valueOf(oldReviewCount))
                .add(BigDecimal.valueOf(newRating));

        BigDecimal newRatingAverage = totalScore.divide(
                BigDecimal.valueOf(oldReviewCount + 1L),
                3,
                RoundingMode.HALF_UP
        );

        product.setRatingAverage(newRatingAverage);
        product.setReviewCount(oldReviewCount + 1);
        productRepository.save(product);
    }

    @Transactional
    protected void syncProductRating(Integer productId) {
        if (productId == null) return;
        Double avg = reviewRepository.avgRatingByProductId(productId);
        Long count = reviewRepository.countByProductId(productId);
        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product == null) return;
        product.setRatingAverage(BigDecimal.valueOf(avg != null ? avg : 0d).setScale(3, RoundingMode.HALF_UP));
        product.setReviewCount(count != null ? count.intValue() : 0);
        productRepository.save(product);
    }

    private void validateReviewImages(List<Integer> productImageIds) {
        if (productImageIds != null && productImageIds.size() > 5) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi duoc toi da 5 anh danh gia");
        }
    }

    private boolean isEditedOnce(String comment) {
        return comment != null && comment.startsWith(EDIT_MARKER);
    }

    @Transactional
    public ReviewResponse customerReply(Integer userId, Integer reviewId, String reply) {
        ReviewEntity review = reviewRepository.findByIdAndProductDeletedFalse(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong co quyen sua review nay");
        }
        if (review.getShopReply() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Shop chua phan hoi, chua the tra loi");
        }
        review.setReply(reply);
        review.setReplyAt(Instant.now());
        review = reviewRepository.save(review);
        notificationService.notifyUser(
                userId,
                review.getOrderItem() != null ? review.getOrderItem().getOrder() : null,
                "Shop đã nhắn lại",
                "Shop đã gửi tin nhắn mới về đánh giá của bạn cho sản phẩm " + review.getProduct().getName() + ".",
                "review_customer_reply",
                null
        );
        return ReviewResponse.from(review);
    }
}
