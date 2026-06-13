package sneak_shop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.response.AuthResponse;
import sneak_shop.entity.UserEntity;
import sneak_shop.enums.UserGender;
import sneak_shop.repository.UserRepository;
import sneak_shop.security.UserContext;
import sneak_shop.service.CloudinaryStorageService;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserRepository userRepository;
    private final CloudinaryStorageService storageService;

    public UserProfileController(UserRepository userRepository, CloudinaryStorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me(@AuthenticationPrincipal UserContext ctx) {
        UserEntity user = userRepository.findById(ctx.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        return ApiResponse.ok(AuthResponse.from(user));
    }

    @PutMapping("/profile")
    public ApiResponse<AuthResponse> updateProfile(
            @AuthenticationPrincipal UserContext ctx,
            @RequestBody Map<String, String> body
    ) {
        UserEntity user = userRepository.findById(ctx.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        if (body.containsKey("email")) {
            String email = normalize(body.get("email"));
            if (email != null && userRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new AppException(ErrorCode.CONFLICT, "Email da duoc su dung");
            }
            if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
                user.setEmailVerified(false);
            }
            user.setEmail(email);
        }
        if (body.containsKey("fullName") && body.get("fullName") != null && !body.get("fullName").isBlank())
            user.setFullName(body.get("fullName").trim());
        if (body.containsKey("phone"))
            user.setPhone(normalize(body.get("phone")));
        if (body.containsKey("gender")) {
            String gender = normalize(body.get("gender"));
            user.setGender(gender != null ? parseGender(gender) : null);
        }
        if (body.containsKey("birthDate")) {
            String birthDate = normalize(body.get("birthDate"));
            user.setBirthDate(birthDate != null ? parseBirthDate(birthDate) : null);
        }
        if (body.containsKey("avatarUrl"))
            user.setAvatarUrl(body.get("avatarUrl"));
        userRepository.save(user);
        return ApiResponse.ok("Cap nhat thanh cong", AuthResponse.from(user));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal UserContext ctx,
            @RequestParam("file") MultipartFile file
    ) {
        String url = storageService.uploadAvatar(file, ctx.id());
        UserEntity user = userRepository.findById(ctx.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        user.setAvatarUrl(url);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyAccount(@AuthenticationPrincipal UserContext ctx) {
        UserEntity user = userRepository.findById(ctx.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        user.setStatus(sneak_shop.enums.UserStatus.inactive);
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());
        user.setLockReason("Da xoa tai khoan");
        userRepository.save(user);
        return ApiResponse.ok("Da xoa tai khoan");
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserGender parseGender(String value) {
        try {
            return UserGender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Gioi tinh khong hop le");
        }
    }

    private LocalDate parseBirthDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ngay sinh khong hop le");
        }
    }
}
