package sneak_shop.dto.response;

import sneak_shop.entity.FinancialLogEntity;

import java.time.Instant;

public record FinancialLogResponse(
        Integer id,
        String email,
        Integer usersId,
        Integer addressesId,
        String addressText,
        Integer ordersId,
        Integer transactionsId,
        Integer productsId,
        Integer productsShopId,
        Long amount,
        String bankName,
        String note,
        Instant createdAt
) {
    public static FinancialLogResponse from(FinancialLogEntity e) {
        return new FinancialLogResponse(
                e.getId(), e.getEmail(),
                e.getUsersId(), e.getAddressesId(),
                null,
                e.getOrdersId(), e.getTransactionsId(),
                e.getProductsId(), e.getProductsShopId(),
                e.getAmount(), e.getBankName(), e.getNote(), e.getCreatedAt()
        );
    }

    public FinancialLogResponse withAddressText(String addressText) {
        return new FinancialLogResponse(
                id, email, usersId, addressesId, addressText,
                ordersId, transactionsId, productsId, productsShopId,
                amount, bankName, note, createdAt
        );
    }
}
