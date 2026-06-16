

package sneak_shop.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.dto.request.LoginRequest;
import sneak_shop.dto.request.RegisterEmailRequest;
import sneak_shop.dto.request.PhoneRegisterRequest;
import sneak_shop.dto.request.RegisterRequest;
import sneak_shop.dto.response.AuthResponse;
import sneak_shop.entity.UserEntity;
import sneak_shop.enums.UserStatus;
import sneak_shop.repository.UserRepository;
import sneak_shop.security.JwtService;
import sneak_shop.service.AuthService;
import sneak_shop.service.PasswordResetService;
import sneak_shop.service.PhoneOtpService;
import sneak_shop.service.UsernameGenerator;
import sneak_shop.websocket.RealtimeSocketHub;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String PRIMARY_ADMIN_EMAIL = "phamcuong26.dev@gmail.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final PhoneOtpService phoneOtpService;
    private final UsernameGenerator usernameGenerator;
    private final RealtimeSocketHub realtimeSocketHub;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, PasswordResetService passwordResetService,
                           PhoneOtpService phoneOtpService, UsernameGenerator usernameGenerator,
                           RealtimeSocketHub realtimeSocketHub) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.phoneOtpService = phoneOtpService;
        this.usernameGenerator = usernameGenerator;
        this.realtimeSocketHub = realtimeSocketHub;
    }

    @Override
    public AuthResponse register(RegisterRequest req) {
        passwordResetService.verifyRegisterOtp(req.email(), req.otp());
        if (req.phone() != null && !req.phone().isBlank() && userRepository.existsByPhone(req.phone())) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại đã được sử dụng");
        }
        UserEntity user = UserEntity.builder()
                .email(req.email())
                .fullName(req.fullName())
                .phone(req.phone())
                .username(usernameGenerator.generateFromEmail(req.email()))
                .password(passwordEncoder.encode(req.password()))
                .emailVerified(true)
                .build();
        user = saveWithRetry(user, req.email());
        realtimeSocketHub.afterCommit(() -> realtimeSocketHub.pushAdminDashboardRefresh("user_created"));
        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    public AuthResponse registerEmail(RegisterEmailRequest req) {
        passwordResetService.consumeVerifiedRegisterEmail(req.email());
        if (req.phone() != null && !req.phone().isBlank() && userRepository.existsByPhone(req.phone())) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại đã được sử dụng");
        }
        UserEntity user = UserEntity.builder()
                .email(req.email())
                .fullName(req.fullName())
                .phone(req.phone())
                .username(usernameGenerator.generateFromEmail(req.email()))
                .password(passwordEncoder.encode(req.password()))
                .emailVerified(true)
                .build();
        user = saveWithRetry(user, req.email());
        realtimeSocketHub.afterCommit(() -> realtimeSocketHub.pushAdminDashboardRefresh("user_created"));
        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    public AuthResponse registerPhone(PhoneRegisterRequest req) {
        String normalized = phoneOtpService.normalizePhone(req.phone());
        if (userRepository.existsByPhone(req.phone()) || userRepository.existsByPhone(normalized)) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại đã được sử dụng");
        }
        phoneOtpService.consumeVerifiedPhone(req.phone());
        UserEntity user = UserEntity.builder()
                .phone(req.phone())
                .fullName(req.fullName())
                .username(usernameGenerator.generate(req.fullName(), req.phone()))
                .password(passwordEncoder.encode(req.password()))
                .build();
        user = saveWithRetry(user, req.phone());
        realtimeSocketHub.afterCommit(() -> realtimeSocketHub.pushAdminDashboardRefresh("user_created"));
        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    public void verifyPhoneOtp(String phone, String otp) {
        phoneOtpService.verifyAndMark(phone, otp);
    }

    @Override
    public void sendEmailVerificationOtp(String email) {
        passwordResetService.sendEmailVerificationOtp(email);
    }

    @Override
    public void verifyEmailVerificationOtp(String email, String otp) {
        passwordResetService.verifyEmailVerificationOtp(email, otp);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        String identity = req.identity().trim();
        UserEntity user;

        boolean looksLikePhone = identity.matches("^[0-9+\\s\\-]{9,15}$");
        if (looksLikePhone) {
            String normalized = phoneOtpService.normalizePhone(identity);
            user = userRepository.findByPhone(identity)
                    .or(() -> userRepository.findByPhone(normalized))
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Thông tin đăng nhập không đúng"));
        } else {
            user = userRepository.findByUsername(identity)
                    .or(() -> userRepository.findByEmail(identity))
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Thông tin đăng nhập không đúng"));
        }

        if (user.getDeletedAt() != null || user.getStatus() == UserStatus.inactive || Boolean.FALSE.equals(user.getEnabled())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, lockedMessage(user));
        }

        if (user.getPassword() == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Thông tin đăng nhập không đúng");
        }

        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthResponse googleLogin(String idToken, String accessToken) {
        RestTemplate rest = new RestTemplate();
        Map<String, Object> info;
        try {
            if (idToken != null && !idToken.isBlank()) {
                info = rest.getForObject(
                        "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                        Map.class
                );
            } else if (accessToken != null && !accessToken.isBlank()) {
                var headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(accessToken);
                var entity = new org.springframework.http.HttpEntity<Void>(headers);
                var response = rest.exchange(
                        "https://www.googleapis.com/oauth2/v3/userinfo",
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        Map.class
                );
                info = response.getBody();
            } else {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Thieu token Google");
            }
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Token Google không hợp lệ");
        }
        if (info == null || info.containsKey("error")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Token Google không hợp lệ");
        }

        String rawEmail = (String) info.get("email");
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Google không trả về email");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        final String googleEmail = email;
        final String name = (String) info.getOrDefault("name", googleEmail);

        if (userRepository.existsByEmail(googleEmail)) {
            throw new AppException(ErrorCode.CONFLICT, "Email này đã được đăng ký. Vui lòng đăng nhập.");
        }

        UserEntity user = UserEntity.builder()
                .email(googleEmail)
                .fullName(name)
                .username(usernameGenerator.generateFromEmail(googleEmail))
                .emailVerified(true)
                .role(googleEmail.equals(PRIMARY_ADMIN_EMAIL) ? sneak_shop.enums.UserRole.admin : sneak_shop.enums.UserRole.user)
                .build();
        user = saveWithRetry(user, googleEmail);

        realtimeSocketHub.afterCommit(() -> realtimeSocketHub.pushAdminDashboardRefresh("user_created"));
        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthResponse googleLoginOnly(String idToken, String accessToken) {
        RestTemplate rest = new RestTemplate();
        Map<String, Object> info;
        try {
            if (idToken != null && !idToken.isBlank()) {
                info = rest.getForObject(
                        "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                        Map.class
                );
            } else if (accessToken != null && !accessToken.isBlank()) {
                var headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(accessToken);
                var entity = new org.springframework.http.HttpEntity<Void>(headers);
                var response = rest.exchange(
                        "https://www.googleapis.com/oauth2/v3/userinfo",
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        Map.class
                );
                info = response.getBody();
            } else {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Thiếu token Google");
            }
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Token Google không hợp lệ");
        }
        if (info == null || info.containsKey("error")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Token Google không hợp lệ");
        }

        String rawEmail = (String) info.get("email");
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Google không trả về email");
        }
        String googleEmail = rawEmail.trim().toLowerCase(Locale.ROOT);

        UserEntity user = userRepository.findByEmail(googleEmail)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản chưa được đăng ký. Vui lòng đăng ký trước."));

        if (user.getDeletedAt() != null || user.getStatus() == UserStatus.inactive || Boolean.FALSE.equals(user.getEnabled())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, lockedMessage(user));
        }
        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    public AuthResponse me(Integer userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        return AuthResponse.from(user);
    }

    private static final DateTimeFormatter LOCK_TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private String lockedMessage(UserEntity user) {
        if (Boolean.FALSE.equals(user.getEnabled()) || user.getStatus() == UserStatus.inactive) {
            return "Tài khoản đã bị xóa";
        }
        String lockedAt = "";
        if (user.getDeletedAt() != null) {
            ZonedDateTime vn = user.getDeletedAt().atZone(VN_ZONE);
            lockedAt = " lúc " + LOCK_TIME_FMT.format(vn);
        }
        if (user.getLockReason() != null && !user.getLockReason().isBlank()) {
            return "Tài khoản đã bị khóa" + lockedAt + ". Lý do: " + user.getLockReason();
        }
        return "Tài khoản đã bị khóa" + lockedAt;
    }

    private UserEntity saveWithRetry(UserEntity user, String seed) {
        String baseSeed = seed == null || seed.isBlank() ? "user" : seed;
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                return userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                if (attempt == 7) {
                    throw new AppException(ErrorCode.CONFLICT, "Không thể tạo tài khoản, vui lòng thử lại");
                }
                String nextSeed = baseSeed + "_" + randomSuffix();
                user.setUsername(usernameGenerator.generate(nextSeed));
            }
        }
        throw new AppException(ErrorCode.CONFLICT, "Không thể tạo tài khoản, vui lòng thử lại");
    }

    private String randomSuffix() {
        return Long.toString(ThreadLocalRandom.current().nextLong(1000, 999999), 36);
    }
}
