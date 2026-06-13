package sneak_shop.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${app.cloudinary.url:}") String cloudinaryUrl,
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret
    ) {
        String normalizedUrl = cloudinaryUrl == null ? "" : cloudinaryUrl.trim();
        if (!normalizedUrl.isBlank()) {
            try {
                return new Cloudinary(normalizedUrl);
            } catch (RuntimeException ignored) {
                Map<String, String> parsed = parseCloudinaryUrl(normalizedUrl);
                if (parsed != null) {
                    return new Cloudinary(Map.of(
                            "cloud_name", parsed.get("cloud_name"),
                            "api_key", parsed.get("api_key"),
                            "api_secret", parsed.get("api_secret"),
                            "secure", true
                    ));
                }
            }
        }
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    private Map<String, String> parseCloudinaryUrl(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (!trimmed.startsWith("cloudinary://")) return null;

        String payload = trimmed.substring("cloudinary://".length());
        int colon = payload.indexOf(':');
        int at = payload.indexOf('@', colon + 1);
        if (colon < 0 || at < 0) return null;

        String apiKey = payload.substring(0, colon).trim();
        String apiSecret = payload.substring(colon + 1, at).trim();
        String cloudName = payload.substring(at + 1).trim();
        if (apiKey.isBlank() || apiSecret.isBlank() || cloudName.isBlank()) return null;

        return Map.of(
                "api_key", apiKey,
                "api_secret", apiSecret,
                "cloud_name", cloudName
        );
    }
}
