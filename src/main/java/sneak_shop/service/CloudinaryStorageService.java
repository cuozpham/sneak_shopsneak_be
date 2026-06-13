package sneak_shop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final String cloudinaryUrl;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    public CloudinaryStorageService(
            Cloudinary cloudinary,
            @Value("${app.cloudinary.url:}") String cloudinaryUrl,
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.cloudinary.folder:sneak-shop}") String folder
    ) {
        this.cloudinary = cloudinary;
        this.cloudinaryUrl = cloudinaryUrl;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = folder;
    }

    public String uploadMedia(MultipartFile file) {
        validate(file, true);
        return upload(file, folder);
    }

    public String uploadAvatar(MultipartFile file, Integer userId) {
        validate(file, false);
        return upload(file, folder + "/avatars/" + userId);
    }

    private String upload(MultipartFile file, String targetFolder) {
        ensureConfigured();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", targetFolder,
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Cloudinary khong tra ve URL");
            }
            return secureUrl.toString();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Khong the tai file len Cloudinary");
        }
    }

    private void validate(MultipartFile file, boolean allowVideo) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "File rong");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "File toi da 20MB");
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && contentType.startsWith("image/");
        boolean isVideo = contentType != null && contentType.startsWith("video/");
        if (!isImage && !(allowVideo && isVideo)) {
            String message = allowVideo
                    ? "Chi chap nhan file anh hoac video"
                    : "Chi chap nhan file anh";
            throw new AppException(ErrorCode.INVALID_REQUEST, message);
        }
    }

    private void ensureConfigured() {
        boolean hasCloudinaryUrl = !cloudinaryUrl.isBlank();
        boolean hasSeparateCredentials = !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
        if (!hasCloudinaryUrl && !hasSeparateCredentials) {
            throw new AppException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Cloudinary chua duoc cau hinh"
            );
        }
    }
}
