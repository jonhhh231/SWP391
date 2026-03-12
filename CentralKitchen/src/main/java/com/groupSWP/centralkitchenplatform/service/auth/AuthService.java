package com.groupSWP.centralkitchenplatform.service.auth;

import com.groupSWP.centralkitchenplatform.dto.auth.*;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.auth.SystemUserRepository;
import com.groupSWP.centralkitchenplatform.repositories.store.StoreRepository;
import com.groupSWP.centralkitchenplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final SystemUserRepository systemUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private final OtpService otpService;
    private final MailService mailService;

    private final StoreRepository storeRepository;

    public AuthResponse login(AuthRequest request) {
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mật khẩu!");
        }

        SystemUser profile = account.getSystemUser();
        // CẬP NHẬT: Thêm .isBlank() để chặn trường hợp email rỗng ""
        if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản chưa cập nhật Email để nhận mã OTP!");
        }

        String otp = otpService.generateOtp(account.getUsername());
        mailService.sendOtpMail(profile.getEmail(), otp);

        return AuthResponse.builder()
                .username(account.getUsername())
                .message("OTP_REQUIRED")
                .build();
    }

    public AuthResponse verifyOtp(String username, String otp) {
        if (!otpService.validateOtp(username, otp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mã OTP không chính xác hoặc đã hết hạn!");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        otpService.clearOtp(username);

        String token = jwtService.generateToken(account);
        // 2. LƯU TOKEN VÀO DATABASE (Đè token cũ nếu có)
        account.setActiveToken(token);
        accountRepository.save(account);

        return AuthResponse.builder()
                .token(token)
                .username(account.getUsername())
                // Lưu ý: Nếu AuthResponse DTO đang nhận String thì dùng account.getRole().name() ở đây
                .role(account.getRole().name())
                .message("Login Success")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        // 1. Kiểm tra Username trùng lặp
        if (accountRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username này đã tồn tại trong hệ thống!");
        }
        if (!org.springframework.util.StringUtils.hasText(request.email())) {
            throw new RuntimeException("Email không được để trống! Cần có email hợp lệ để nhận mã OTP.");
        }
        String cleanEmail = request.email().trim();
        if (systemUserRepository.findByEmail(cleanEmail).isPresent()) {
            throw new RuntimeException("Email này đã được sử dụng cho một tài khoản khác!");
        }

        // ==========================================
        // 🛑 TRẠM KIỂM SOÁT VÀ TÌM CỬA HÀNG (ĐÃ MỞ CHỐT CHO PHÉP OPTIONAL)
        // ==========================================
        com.groupSWP.centralkitchenplatform.entities.auth.Store store = null;

        if (request.role() == Account.Role.STORE_MANAGER) {
            // Nếu CÓ truyền storeId lên thì mới đi tìm và gán cửa hàng
            if (request.storeId() != null && !request.storeId().isBlank()) {
                store = storeRepository.findById(request.storeId())
                        .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại (Sai storeId)!"));

                if (store.getAccount() != null) {
                    throw new RuntimeException("Cửa hàng [" + store.getName() + "] đã có Quản lý rồi! Không thể bổ nhiệm thêm.");
                }
            }
            // NẾU KHÔNG TRUYỀN: Bỏ qua luôn, không throw lỗi nữa -> Tạo ra "Người rảnh" (Dự bị)
        }
        // ==========================================

        // 3. Tạo Account
        Account account = new Account();
        account.setUsername(request.username());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(request.role());

        // Nếu có store thì gán, không có thì nó mặc định là null dưới DB
        if (store != null) {
            account.setStore(store);
        }

        account = accountRepository.save(account);

        // 4. Tạo Hồ sơ SystemUser
        SystemUser userProfile = SystemUser.builder()
                .userId(generateStaffId(request.role()))
                .fullName(request.fullName())
                .email(cleanEmail)
                .account(account)
                .build();

        systemUserRepository.save(userProfile);

        // Đổi câu thông báo một chút cho hợp lý
        String message = "Đăng ký thành công! Mã nhân viên: " + userProfile.getUserId();
        if (store != null) {
            message += " | Đã bổ nhiệm quản lý Cửa hàng: " + store.getName();
        } else if (request.role() == Account.Role.STORE_MANAGER) {
            message += " | Nhân sự đang ở trạng thái DỰ BỊ (Chưa gán cửa hàng).";
        }

        return message;
    }

    // ĐÃ SỬA: Tham số truyền vào là Account.Role
    private String generateStaffId(Account.Role role) {
        String prefix = getPrefixByRole(role);
        Optional<String> lastUserId = systemUserRepository.findLastUserIdByRole(role);
        if (lastUserId.isEmpty()) return prefix + "00001";

        String lastId = lastUserId.get();
        String numberPart = lastId.substring(prefix.length());
        try {
            int number = Integer.parseInt(numberPart);
            return prefix + String.format("%05d", ++number);
        } catch (Exception e) {
            return prefix + System.currentTimeMillis();
        }
    }

    // ĐÃ SỬA: Tham số truyền vào là Account.Role
    private String getPrefixByRole(Account.Role role) {
        if (role == null) return "USR";
        return switch (role) {
            case ADMIN -> "ADM";
            case MANAGER -> "MNG";
            case COORDINATOR -> "COR";
            case KITCHEN_MANAGER -> "KIT";
            case STORE_MANAGER -> "STR";
            default -> "USR";
        };
    }

    @Transactional
    public SystemUser updateProfile(String currentUsername, UpdateProfileRequest request) {
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        SystemUser profile = account.getSystemUser();

        if (org.springframework.util.StringUtils.hasText(request.getFullName())) {
            profile.setFullName(request.getFullName().trim());
        }

        if (org.springframework.util.StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim();

            if (!newEmail.equalsIgnoreCase(profile.getEmail())) {
                boolean isEmailTaken = systemUserRepository.findByEmail(newEmail).isPresent();
                if (isEmailTaken) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng bởi một tài khoản khác!");
                }
                profile.setEmail(newEmail);
            }
        }

        return systemUserRepository.save(profile);
    }

    public void forgotPassword(String email) {
        SystemUser profile = systemUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống!"));

        String otp = otpService.generateOtp(email);
        mailService.sendOtpMail(email, otp);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String otp, String newPassword) {
        boolean isValid = otpService.validateOtp(email, otp);

        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mã OTP không chính xác hoặc đã hết hạn!");
        }

        SystemUser profile = systemUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại!"));

        Account account = profile.getAccount();
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu tài khoản bị lỗi, không tìm thấy Account!");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        otpService.clearOtp(email);
    }

    @Transactional
    public void logout(String username) {
        accountRepository.findByUsername(username).ifPresent(account -> {
            account.setActiveToken(null);
            accountRepository.save(account);
        });
    }
}