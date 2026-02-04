package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.auth.AuthRequest;
import com.groupSWP.centralkitchenplatform.dto.auth.AuthResponse;
import com.groupSWP.centralkitchenplatform.dto.auth.RegisterRequest;
import com.groupSWP.centralkitchenplatform.dto.auth.UpdateProfileRequest;
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
        // Với JWT, client chỉ cần xóa token ở trình duyệt.
        // Backend trả về JSON xác nhận.
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    // API Đăng ký tài khoản mới (Public)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        // Gọi xuống Service để xử lý
        String result = authService.register(request);

        // Trả về 200 OK kèm thông báo
        return ResponseEntity.ok(result);
    }

    // API Cập nhật thông tin bản thân
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request, Principal principal) {
        // principal.getName() sẽ tự động lấy username từ cái Token mà người dùng gửi lên
        SystemUser updatedUser = authService.updateProfile(principal.getName(), request);

        return ResponseEntity.ok(updatedUser);
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
}