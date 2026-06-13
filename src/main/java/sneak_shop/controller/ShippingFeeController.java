package sneak_shop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.response.CurrentShippingFeeResponse;
import sneak_shop.service.ShippingFeeService;

@RestController
@RequestMapping("/api/shipping-fees")
public class ShippingFeeController {

    private final ShippingFeeService shippingFeeService;

    public ShippingFeeController(ShippingFeeService shippingFeeService) {
        this.shippingFeeService = shippingFeeService;
    }

    @GetMapping("/current")
    public ApiResponse<CurrentShippingFeeResponse> getCurrent() {
        return ApiResponse.ok(shippingFeeService.getCurrent());
    }
}
