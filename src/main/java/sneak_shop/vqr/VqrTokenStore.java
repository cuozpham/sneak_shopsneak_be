package sneak_shop.vqr;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VqrTokenStore {

    private static final long TTL_SECONDS = 3600;

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public String issueToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, Instant.now().plusSeconds(TTL_SECONDS).toEpochMilli());
        return token;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < Instant.now().toEpochMilli()) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}
