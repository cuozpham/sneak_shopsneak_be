package sneak_shop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private record OtpEntry(String otp, Instant expiresAt) {}
    private record ResetTokenEntry(String email, Instant expiresAt) {}

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, ResetTokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> registerOtpStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedRegisterEmailStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> emailVerifyOtpStore = new ConcurrentHashMap<>();

    private record EmailChangeEntry(String newEmail, String otp, Instant expiresAt) {}
    private final Map<Integer, EmailChangeEntry> emailChangeOtpStore = new ConcurrentHashMap<>();

    public void sendEmailChangeOtp(Integer userId, String newEmail) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Email mới không được để trống");
        }
        String email = newEmail.trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Email không hợp lệ");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài khoản không tồn tại"));
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Email mới trùng với email hiện tại");
        }
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new AppException(ErrorCode.CONFLICT, "Email đã được sử dụng bởi tài khoản khác");
        }
        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        emailChangeOtpStore.put(userId, new EmailChangeEntry(email, otp, Instant.now().plusSeconds(300)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[MANDRO] Mã xác nhận đổi email");
        msg.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Bạn vừa yêu cầu đổi email đăng nhập MANDRO sang địa chỉ này.\n"
                + "Mã OTP xác nhận của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Nếu không phải bạn, vui lòng bỏ qua email này.\n\n"
                + "MANDRO");
        sendMailAsync(msg);
    }

    private void sendMailAsync(SimpleMailMessage msg) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                mailSender.send(msg);
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(PasswordResetService.class)
                        .warn("Failed to send mail to {}: {}", msg.getTo(), ex.getMessage());
            }
        });
    }

    public String confirmEmailChange(Integer userId, String newEmail, String otp) {
        EmailChangeEntry entry = emailChangeOtpStore.get(userId);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng gửi lại");
        }
        if (!entry.newEmail().equalsIgnoreCase(newEmail.trim())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Email không khớp với yêu cầu");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        emailChangeOtpStore.remove(userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài khoản không tồn tại"));
        if (userRepository.existsByEmailAndIdNot(entry.newEmail(), userId)) {
            throw new AppException(ErrorCode.CONFLICT, "Email đã được sử dụng bởi tài khoản khác");
        }
        user.setEmail(entry.newEmail());
        user.setEmailVerified(true);
        userRepository.save(user);
        return entry.newEmail();
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public void sendRegisterOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.CONFLICT, "Email đã được sử dụng");
        }
        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        registerOtpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));
        verifiedRegisterEmailStore.remove(email);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[MANDRO] Mã xác nhận đăng ký tài khoản");
        msg.setText("Xin chào,\n\n"
                + "Mã OTP xác nhận đăng ký tài khoản MANDRO của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.\n\n"
                + "MANDRO");
        sendMailAsync(msg);
    }

    public void verifyRegisterOtp(String email, String otp) {
        OtpEntry entry = registerOtpStore.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        registerOtpStore.remove(email);
        verifiedRegisterEmailStore.put(email, Instant.now().plusSeconds(600));
    }

    public void consumeVerifiedRegisterEmail(String email) {
        Instant expiresAt = verifiedRegisterEmailStore.get(email);
        if (expiresAt == null || Instant.now().isAfter(expiresAt)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Email chưa được xác thực OTP");
        }
        verifiedRegisterEmailStore.remove(email);
    }

    public void sendEmailVerificationOtp(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Email không tồn tại trong hệ thống"));
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));
        emailVerifyOtpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[MANDRO] Mã xác thực email");
        msg.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Mã OTP để xác thực email của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.\n\n"
                + "MANDRO");
        sendMailAsync(msg);
    }

    public void verifyEmailVerificationOtp(String email, String otp) {
        OtpEntry entry = emailVerifyOtpStore.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        emailVerifyOtpStore.remove(email);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài khoản không tồn tại"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void sendOtp(String email, String phone, String fullName) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Email không tồn tại trong hệ thống"));

        if (!phone.equals(user.getPhone())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thông tin không khớp");
        }
        if (!fullName.trim().equalsIgnoreCase(user.getFullName().trim())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thông tin không khớp");
        }

        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        otpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[MANDRO] Mã xác nhận đặt lại mật khẩu");
        msg.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Mã OTP để đặt lại mật khẩu của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.\n\n"
                + "MANDRO");
        sendMailAsync(msg);
    }

    public String verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        otpStore.remove(email);

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new ResetTokenEntry(email, Instant.now().plusSeconds(600)));
        return token;
    }

    public void resetPassword(String token, String newPassword) {
        ResetTokenEntry entry = tokenStore.get(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Token đã hết hạn, vui lòng bắt đầu lại");
        }
        tokenStore.remove(token);

        UserEntity user = userRepository.findByEmail(entry.email())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài khoản không tồn tại"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
