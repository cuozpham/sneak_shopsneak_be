package sneak_shop.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ShippingFeeConfigRequest(
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM") YearMonth month,
        @NotNull @DecimalMin(value = "0.0") @Digits(integer = 13, fraction = 2) BigDecimal fee
) {}
