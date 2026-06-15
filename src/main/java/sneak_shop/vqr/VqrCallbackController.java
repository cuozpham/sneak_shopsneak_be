package sneak_shop.vqr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sneak_shop.entity.VqrTransactionLog;
import sneak_shop.repository.VqrTransactionLogRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/vqr/bank/api/test")
public class VqrCallbackController {

    private final VqrTokenStore tokenStore;
    private final VqrTransactionLogRepository repository;
    private final ObjectMapper objectMapper;

    public VqrCallbackController(VqrTokenStore tokenStore,
                                 VqrTransactionLogRepository repository,
                                 ObjectMapper objectMapper) {
        this.tokenStore = tokenStore;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/transaction-callback")
    public ResponseEntity<?> transactionCallback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload
    ) {
        String bearerToken = extractBearerToken(authorization);
        if (!tokenStore.isValid(bearerToken)) {
            Map<String, String> response = new LinkedHashMap<>();
            response.put("code", "01");
            response.put("desc", "unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        log.info("VQR transaction callback received: {}", payload);

        VqrTransactionLog logEntity = VqrTransactionLog.builder()
                .content(asString(payload.get("content")))
                .amount(asBigDecimal(payload.get("amount")))
                .bankAccount(asString(payload.get("bankAccount")))
                .transactionDate(asString(payload.get("transactionDate")))
                .rawPayload(toJson(payload))
                .receivedAt(LocalDateTime.now())
                .build();
        repository.save(logEntity);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("code", "00");
        response.put("desc", "success");
        return ResponseEntity.ok(response);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return payload.toString();
        }
    }
}
