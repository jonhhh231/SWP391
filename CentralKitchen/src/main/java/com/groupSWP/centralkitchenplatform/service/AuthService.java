package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.auth.*;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.SystemUserRepository;
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
        return AuthResponse.builder()
                .token(token)
                .username(account.getUsername())
                .role(account.getRole())
                .message("Login Success")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        if (accountRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username này đã tồn tại!");
        }

        Account account = new Account();
        account.setUsername(request.username());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(request.role().name());
        account = accountRepository.save(account);

        SystemUser userProfile = SystemUser.builder()
                .userId(generateStaffId(request.role()))
                .fullName(request.fullName())
                .email(request.email())
                .role(request.role())
                .account(account)
                .build();

        systemUserRepository.save(userProfile);
        return "Đăng ký thành công! Mã nhân viên: " + userProfile.getUserId();
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
            case KITCHEN_STAFF -> "KIT";
            case STORE_STAFF -> "STR";
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
}