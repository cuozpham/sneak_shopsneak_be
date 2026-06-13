package sneak_shop.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.ShippingFeeConfigRequest;
import sneak_shop.dto.response.ShippingFeeConfigResponse;
import sneak_shop.entity.ShippingFeeConfigEntity;
import sneak_shop.repository.ShippingFeeConfigRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class ShippingFeeService {

    public static final BigDecimal DEFAULT_FEE = new BigDecimal("30000");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShippingFeeConfigRepository repository;

    public ShippingFeeService(ShippingFeeConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentFee() {
        return getFeeFor(YearMonth.now(BUSINESS_ZONE));
    }

    @Transactional(readOnly = true)
    public BigDecimal getFeeFor(YearMonth month) {
        LocalDate effectiveMonth = month.atDay(1);
        return repository
                .findFirstByEffectiveMonthLessThanEqualOrderByEffectiveMonthDesc(effectiveMonth)
                .map(ShippingFeeConfigEntity::getFee)
                .orElse(DEFAULT_FEE);
    }

    @Transactional(readOnly = true)
    public PageResponse<ShippingFeeConfigResponse> getAll(int page, int size) {
        return PageResponse.from(
                repository.findAllByOrderByEffectiveMonthDesc(PageRequest.of(page, size))
                        .map(ShippingFeeConfigResponse::from)
        );
    }

    @Transactional
    public ShippingFeeConfigResponse save(ShippingFeeConfigRequest request) {
        LocalDate effectiveMonth = request.month().atDay(1);
        ShippingFeeConfigEntity entity = repository.findByEffectiveMonth(effectiveMonth)
                .orElseGet(() -> ShippingFeeConfigEntity.builder()
                        .effectiveMonth(effectiveMonth)
                        .build());
        entity.setFee(request.fee());
        return ShippingFeeConfigResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Cau hinh phi van chuyen khong ton tai");
        }
        repository.deleteById(id);
    }
}
