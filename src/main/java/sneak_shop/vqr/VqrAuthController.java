package sneak_shop.vqr;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/vqr")
public class VqrAuthController {

    private final String expectedUsername;
    private final String expectedPassword;
    private final VqrTokenStore tokenStore;

    public VqrAuthController(
            @Value("${vietqr.callback.username:}") String expectedUsername,
            @Value("${vietqr.callback.password:}") String expectedPassword,
            VqrTokenStore tokenStore
    ) {
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/api/token_generate")
    public ResponseEntity<?> generateToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!isBasicAuthValid(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenStore.issueToken();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        return ResponseEntity.ok(response);
    }

    private boolean isBasicAuthValid(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return false;
            }
            String username = decoded.substring(0, separator);
            String password = decoded.substring(separator + 1);
            return expectedUsername != null
                    && expectedPassword != null
                    && expectedUsername.equals(username)
                    && expectedPassword.equals(password);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
