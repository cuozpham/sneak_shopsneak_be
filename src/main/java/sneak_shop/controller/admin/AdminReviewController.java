package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ShopReplyRequest;
import sneak_shop.dto.response.ReviewResponse;
import sneak_shop.service.impl.ReviewServiceImpl;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewServiceImpl reviewService;

    public AdminReviewController(ReviewServiceImpl reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ApiResponse.ok(reviewService.getAll(page, size, rating, fromDate, toDate));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> getByProduct(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(reviewService.getByProduct(productId, page, size));
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> getById(@PathVariable Integer reviewId) {
        return ApiResponse.ok(reviewService.getById(reviewId));
    }

    @PostMapping("/{reviewId}/reply")
    public ApiResponse<ReviewResponse> reply(@PathVariable Integer reviewId,
                                             @Valid @RequestBody ShopReplyRequest req) {
        return ApiResponse.ok(reviewService.shopReply(reviewId, req));
    }
}
