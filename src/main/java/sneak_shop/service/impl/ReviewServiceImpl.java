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
import java.util.List;
import java.util.Objects;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

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
    public PageResponse<ReviewResponse> getAll(int page, int size) {
        return PageResponse.from(reviewRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)).map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByProduct(Integer productId, int page, int size) {
        return PageResponse.from(reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(Integer userId, int page, int size) {
        return PageResponse.from(reviewRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)).map(ReviewResponse::from));
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
        ProductEntity product = productRepository.findById(orderItem.getProduct().getId())
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
                .editCount(0)
                .build();
        review = reviewRepository.save(review);

        replaceReviewImages(review, req.productImageIds());
        try {
            notificationService.notifyAdmins(
                    "Co danh gia moi",
                    "San pham " + product.getName() + " vua co mot danh gia moi.",
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
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong co quyen sua review nay");
        }
        if (review.getShopReply() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Da co phan hoi cua shop, khong the sua danh gia");
        }
        if (review.getEditCount() != null && review.getEditCount() >= 1) {
            throw new AppException(ErrorCode.CONFLICT, "Ban chi duoc sua danh gia mot lan");
        }
        Instant editableUntil = review.getCreatedAt().plus(Duration.ofHours(24));
        if (Instant.now().isAfter(editableUntil)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi co the sua danh gia trong 24 gio dau");
        }

        if (req.rating() != null) review.setRating(req.rating());
        review.setComment(req.comment());
        review.setEditCount((review.getEditCount() == null ? 0 : review.getEditCount()) + 1);
        review = reviewRepository.save(review);
        replaceReviewImages(review, req.productImageIds());
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
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        review.setShopReply(req.reply());
        review.setShopReplyAt(Instant.now());
        review = reviewRepository.save(review);
        notificationService.notifyUser(
                review.getUser().getId(),
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

    private void validateReviewImages(List<Integer> productImageIds) {
        if (productImageIds != null && productImageIds.size() > 5) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chi duoc toi da 5 anh danh gia");
        }
    }

    @Transactional
    public ReviewResponse customerReply(Integer userId, Integer reviewId, String reply) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review khong ton tai"));
        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Khong co quyen sua review nay");
        }
        if (review.getShopReply() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Shop chua phan hoi, chua the tra loi");
        }
        review.setReply(reply);
        review.setReplyAt(Instant.now());
        return ReviewResponse.from(reviewRepository.save(review));
    }
}
