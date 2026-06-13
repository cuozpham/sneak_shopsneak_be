package sneak_shop.config;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigTest {

    private final CloudinaryConfig config = new CloudinaryConfig();

    @Test
    void fallsBackToManualParsingWhenCloudinaryUrlIsNotAValidUri() {
        Cloudinary cloudinary = config.cloudinary(
                "cloudinary://123456:secret[with-bracket@demo-cloud",
                "",
                "",
                ""
        );

        assertThat(cloudinary.config.cloudName).isEqualTo("demo-cloud");
        assertThat(cloudinary.config.apiKey).isEqualTo("123456");
        assertThat(cloudinary.config.apiSecret).isEqualTo("secret[with-bracket");
        assertThat(cloudinary.config.secure).isTrue();
    }

    @Test
    void usesIndividualVariablesWhenCloudinaryUrlIsBlank() {
        Cloudinary cloudinary = config.cloudinary(
                " ",
                "demo-cloud",
                "123456",
                "secret"
        );

        assertThat(cloudinary.config.cloudName).isEqualTo("demo-cloud");
        assertThat(cloudinary.config.apiKey).isEqualTo("123456");
        assertThat(cloudinary.config.apiSecret).isEqualTo("secret");
        assertThat(cloudinary.config.secure).isTrue();
    }
}
