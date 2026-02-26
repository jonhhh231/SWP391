package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.dto.auth.RegisterRequest;
import com.groupSWP.centralkitchenplatform.service.AccountService;
import com.groupSWP.centralkitchenplatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
// 🔒 Chốt chặn cứng: Chỉ ADMIN mới được sờ vào Controller này
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    // API: Admin tạo tài khoản cho nhân viên
    // URL: POST /api/admin/users
    @PostMapping("/users")
    public ResponseEntity<String> createEmployee(@RequestBody RegisterRequest request) {
        // Tận dụng lại hàm register logic cũ, chỉ khác người gọi là Admin
        String result = authService.register(request);
        return ResponseEntity.ok("Admin đã cấp tài khoản thành công! Username: " + request.username());
    }

    private final AccountService accountService;

    // 1. API: Liệt kê danh sách account và thông tin (Trừ ADMIN)
    // Bất kỳ ai đăng nhập (hoặc tùy cấu hình của bạn) đều có thể gọi
    @GetMapping("/list-accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAccountsExcludingAdmin());
    }
}