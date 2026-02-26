package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.auth.*;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Principal principal
    ) {
        SystemUser updatedUser = authService.updateProfile(principal.getName(), request);

        UserResponse response = UserResponse.builder()
                .userId(updatedUser.getUserId())
                .fullName(updatedUser.getFullName())
                .role(updatedUser.getRole().name())
                .username(principal.getName())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-me")
    public ResponseEntity<?> checkMe(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa xác thực (Authentication is null)");
        }

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("username", authentication.getName());
        debugInfo.put("authorities", authentication.getAuthorities());
        debugInfo.put("isAuthenticated", authentication.isAuthenticated());

        return ResponseEntity.ok(debugInfo);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request.getUsername(), request.getOtp()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn.");
    }

    // API 2: Nhập mã OTP và Mật khẩu mới (Nhận JSON)
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.ok("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay bây giờ.");
    }
}