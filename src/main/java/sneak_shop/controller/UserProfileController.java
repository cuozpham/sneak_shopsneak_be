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
    private final CloudinaryStorageService cloudinaryStorageService;
    private final sneak_shop.service.PasswordResetService passwordResetService;

    public UserProfileController(UserRepository userRepository,
                                 CloudinaryStorageService cloudinaryStorageService,
                                 sneak_shop.service.PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.passwordResetService = passwordResetService;
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
        // email không sửa qua endpoint này — dùng /profile/email/request và /profile/email/confirm
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
        String url = cloudinaryStorageService.uploadAvatar(file, ctx.id());

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
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(value);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ngay sinh khong hop le");
        }
        if (parsed.isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ngay sinh khong duoc vuot qua ngay hien tai");
        }
        return parsed;
    }

    public record EmailChangeRequest(String email) {}
    public record EmailChangeConfirm(String email, String otp) {}

    @PostMapping("/profile/email/request")
    public ApiResponse<Void> requestEmailChange(@AuthenticationPrincipal UserContext ctx,
                                                @RequestBody EmailChangeRequest req) {
        passwordResetService.sendEmailChangeOtp(ctx.id(), req.email());
        return ApiResponse.ok("Da gui OTP toi email moi");
    }

    @PostMapping("/profile/email/confirm")
    public ApiResponse<AuthResponse> confirmEmailChange(@AuthenticationPrincipal UserContext ctx,
                                                        @RequestBody EmailChangeConfirm req) {
        passwordResetService.confirmEmailChange(ctx.id(), req.email(), req.otp());
        UserEntity user = userRepository.findById(ctx.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        return ApiResponse.ok("Doi email thanh cong", AuthResponse.from(user));
    }
}
