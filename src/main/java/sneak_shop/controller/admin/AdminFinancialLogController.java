package sneak_shop.controller.admin;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.entity.AddressEntity;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.TransactionEntity;
import sneak_shop.dto.response.FinancialLogResponse;
import sneak_shop.entity.FinancialLogEntity;
import sneak_shop.repository.AddressRepository;
import sneak_shop.repository.OrderRepository;
import sneak_shop.repository.FinancialLogRepository;
import sneak_shop.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinancialLogController {

    private final FinancialLogRepository repository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    public AdminFinancialLogController(FinancialLogRepository repository,
                                       AddressRepository addressRepository,
                                       OrderRepository orderRepository,
                                       TransactionRepository transactionRepository) {
        this.repository = repository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<FinancialLogResponse>> getAll(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) Integer ordersId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Integer resolvedOrderId = ordersId;
        if (resolvedOrderId == null && orderCode != null && !orderCode.isBlank()) {
            resolvedOrderId = orderRepository.findByOrderCode(orderCode.trim()).map(OrderEntity::getId).orElse(-1);
        }
        Integer resolvedTransactionId = null;
        if (transactionCode != null && !transactionCode.isBlank()) {
            resolvedTransactionId = transactionRepository.findByTransactionCode(transactionCode.trim())
                    .map(TransactionEntity::getId)
                    .orElse(-1);
        }
        final Integer finalResolvedOrderId = resolvedOrderId;
        final Integer finalResolvedTransactionId = resolvedTransactionId;
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<FinancialLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (email != null && !email.isBlank())
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            if (finalResolvedOrderId != null)
                predicates.add(cb.equal(root.get("ordersId"), finalResolvedOrderId));
            if (finalResolvedTransactionId != null)
                predicates.add(cb.equal(root.get("transactionsId"), finalResolvedTransactionId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ApiResponse.ok(PageResponse.from(repository.findAll(spec, pageable).map(log ->
                enrichResponse(log)
        )));
    }

    @PostMapping
    public ApiResponse<FinancialLogResponse> create(@RequestBody FinancialLogEntity req) {
        req.setId(null);
        FinancialLogEntity saved = repository.save(req);
        return ApiResponse.ok(enrichResponse(saved));
    }

    private FinancialLogResponse enrichResponse(FinancialLogEntity log) {
        FinancialLogResponse response = FinancialLogResponse.from(log).withAddressText(resolveAddressText(log));
        if (log.getOrdersId() != null) {
            response = response.withOrderCode(orderRepository.findById(log.getOrdersId())
                    .map(OrderEntity::getOrderCode)
                    .orElse(null));
        }
        if (log.getTransactionsId() != null) {
            response = response.withTransactionCode(transactionRepository.findById(log.getTransactionsId())
                    .map(TransactionEntity::getTransactionCode)
                    .orElse(null));
        }
        return response;
    }

    private String resolveAddressText(FinancialLogEntity log) {
        if (log.getAddressesId() != null) {
            return addressRepository.findById(log.getAddressesId())
                    .map(this::formatAddress)
                    .orElse(null);
        }
        if (log.getOrdersId() != null) {
            return orderRepository.findById(log.getOrdersId())
                    .map(this::formatOrderAddress)
                    .orElse(null);
        }
        return null;
    }

    private String formatAddress(AddressEntity address) {
        if (address == null) return null;
        StringBuilder sb = new StringBuilder();
        if (address.getRecipientName() != null && !address.getRecipientName().isBlank()) {
            sb.append(address.getRecipientName());
        }
        if (address.getRecipientPhone() != null && !address.getRecipientPhone().isBlank()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(address.getRecipientPhone());
        }
        String detail = joinParts(address.getAddress(), address.getWard(), address.getDistrict(), address.getCity());
        if (detail != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(detail);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String formatOrderAddress(OrderEntity order) {
        if (order == null) return null;
        String detail = joinParts(order.getShippingAddress(), order.getShippingWard(), order.getShippingDistrict(), order.getShippingCity());
        if (detail == null) return null;
        StringBuilder sb = new StringBuilder();
        if (order.getRecipientName() != null && !order.getRecipientName().isBlank()) {
            sb.append(order.getRecipientName());
        }
        if (order.getRecipientPhone() != null && !order.getRecipientPhone().isBlank()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(order.getRecipientPhone());
        }
        if (sb.length() > 0) sb.append(" - ");
        sb.append(detail);
        return sb.toString();
    }

    private String joinParts(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
