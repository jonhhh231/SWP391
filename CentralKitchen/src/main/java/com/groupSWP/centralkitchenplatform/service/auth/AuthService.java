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
                .role(account.getRole())
                .message("Login Success")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        // 1. Kiểm tra Username trùng lặp
        if (accountRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username này đã tồn tại trong hệ thống!");
        }

        // ==========================================
        // 🛑 TRẠM KIỂM SOÁT VÀ TÌM CỬA HÀNG
        // ==========================================
        com.groupSWP.centralkitchenplatform.entities.auth.Store store = null; // Khai báo sẵn

        if (request.role() == SystemUser.SystemRole.STORE_MANAGER) {

            // Luật 1: Phải có mã Cửa hàng
            if (request.storeId() == null || request.storeId().isBlank()) {
                throw new RuntimeException("Tạo tài khoản Cửa hàng trưởng bắt buộc phải truyền storeId!");
            }

            // Luật 2: Cửa hàng phải tồn tại
            store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại (Sai storeId)!"));

            // Luật 3: Kiểm tra Cửa hàng này đã có tài khoản Account nào móc vào chưa
            if (store.getAccount() != null) {
                // 👈 Dùng getName() thay vì getStoreName()
                throw new RuntimeException("Cửa hàng [" + store.getName() + "] đã có Quản lý rồi! Không thể bổ nhiệm thêm.");
            }
        }
        // ==========================================

        // 3. Tạo Account
        Account account = new Account();
        account.setUsername(request.username());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(request.role().name());

        // 👑 NẾU LÀ QUẢN LÝ THÌ GẮN CỬA HÀNG VÀO ACCOUNT LUN TRƯỚC KHI LƯU
        if (store != null) {
            account.setStore(store); // 👈 Bảng Account giữ khóa nên mình set ở đây!
        }

        account = accountRepository.save(account);

        // 4. Tạo Hồ sơ SystemUser
        SystemUser userProfile = SystemUser.builder()
                .userId(generateStaffId(request.role()))
                .fullName(request.fullName())
                .email(request.email())
                .role(request.role())
                .account(account)
                .build();

        systemUserRepository.save(userProfile);

        return "Đăng ký thành công! Mã nhân viên: " + userProfile.getUserId() +
                (request.storeId() != null ? " | Đã bổ nhiệm quản lý Cửa hàng: " + request.storeId() : "");
    }

    private String generateStaffId(SystemUser.SystemRole role) {
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

    private String getPrefixByRole(SystemUser.SystemRole role) {
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

        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());

        return systemUserRepository.save(profile);
    }

    // ==========================================
    // CÁC HÀM QUÊN MẬT KHẨU (ĐÃ ĐƯỢC CHUẨN HÓA)
    // ==========================================

    // 1. Quên mật khẩu: Kiểm tra email -> Tạo OTP -> Gửi mail
    public void forgotPassword(String email) {
        // Sử dụng systemUserRepository thay vì userRepository
        SystemUser profile = systemUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống!"));

        // Tạo OTP lưu vào RAM với key là email
        String otp = otpService.generateOtp(email);

        // Gọi hàm của MailService hiện tại bạn đang có
        mailService.sendOtpMail(email, otp);
    }

    // 2. Đặt lại mật khẩu: Kiểm tra OTP -> Đổi mật khẩu -> Xóa OTP
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String otp, String newPassword) {
        boolean isValid = otpService.validateOtp(email, otp);

        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mã OTP không chính xác hoặc đã hết hạn!");
        }

        // Tìm UserProfile theo email
        SystemUser profile = systemUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại!"));

        // Lấy Account liên kết với Profile này để đổi mật khẩu
        Account account = profile.getAccount();
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu tài khoản bị lỗi, không tìm thấy Account!");
        }

        // Đổi mật khẩu trên bảng Account và lưu lại
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        // Xóa OTP khỏi bộ nhớ để tránh bị dùng lại
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