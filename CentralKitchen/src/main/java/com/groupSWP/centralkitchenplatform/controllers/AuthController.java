package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.auth.AuthRequest;
import com.groupSWP.centralkitchenplatform.dto.auth.AuthResponse;
import com.groupSWP.centralkitchenplatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}