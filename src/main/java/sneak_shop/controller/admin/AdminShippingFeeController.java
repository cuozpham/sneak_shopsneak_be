package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ShippingFeeConfigRequest;
import sneak_shop.dto.response.ShippingFeeConfigResponse;
import sneak_shop.service.ShippingFeeService;

@RestController
@RequestMapping("/api/admin/shipping-fees")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShippingFeeController {

    private final ShippingFeeService shippingFeeService;

    public AdminShippingFeeController(ShippingFeeService shippingFeeService) {
        this.shippingFeeService = shippingFeeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ShippingFeeConfigResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(shippingFeeService.getAll(page, size));
    }

    @PutMapping
    public ApiResponse<ShippingFeeConfigResponse> save(
            @Valid @RequestBody ShippingFeeConfigRequest request
    ) {
        return ApiResponse.ok("Luu phi van chuyen thanh cong", shippingFeeService.save(request));
    }
}
