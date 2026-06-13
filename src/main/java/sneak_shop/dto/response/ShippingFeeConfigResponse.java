package sneak_shop.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import sneak_shop.entity.ShippingFeeConfigEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

public record ShippingFeeConfigResponse(
        Integer id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM") YearMonth month,
        BigDecimal fee,
        Instant createdAt,
        Instant updatedAt
) {
    public static ShippingFeeConfigResponse from(ShippingFeeConfigEntity entity) {
        return new ShippingFeeConfigResponse(
                entity.getId(),
                YearMonth.from(entity.getEffectiveMonth()),
                entity.getFee(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
