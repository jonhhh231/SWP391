package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.auth.ChangePasswordRequest;
import com.groupSWP.centralkitchenplatform.service.PasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/settings")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Principal principal, //Thẻ Căn Cước
            @RequestBody ChangePasswordRequest request) {

        // Lấy username từ Token người đang đăng nhập
        String username = principal.getName();

        try {
            passwordService.changePassword(username, request);
            return ResponseEntity.ok("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
        } catch (RuntimeException e) {
            // Trả về lỗi 400 Bad Request nếu sai pass cũ hoặc không khớp
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}