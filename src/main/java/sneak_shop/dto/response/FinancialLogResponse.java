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
        String orderCode,
        Integer transactionsId,
        String transactionCode,
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
                e.getOrdersId(), null, e.getTransactionsId(), null,
                e.getProductsId(), e.getProductsShopId(),
                e.getAmount(), e.getBankName(), e.getNote(), e.getCreatedAt()
        );
    }

    public FinancialLogResponse withAddressText(String addressText) {
        return new FinancialLogResponse(
                id, email, usersId, addressesId, addressText,
                ordersId, orderCode, transactionsId, transactionCode, productsId, productsShopId,
                amount, bankName, note, createdAt
        );
    }

    public FinancialLogResponse withOrderCode(String orderCode) {
        return new FinancialLogResponse(
                id, email, usersId, addressesId, addressText,
                ordersId, orderCode, transactionsId, transactionCode, productsId, productsShopId,
                amount, bankName, note, createdAt
        );
    }

    public FinancialLogResponse withTransactionCode(String transactionCode) {
        return new FinancialLogResponse(
                id, email, usersId, addressesId, addressText,
                ordersId, orderCode, transactionsId, transactionCode, productsId, productsShopId,
                amount, bankName, note, createdAt
        );
    }
}
