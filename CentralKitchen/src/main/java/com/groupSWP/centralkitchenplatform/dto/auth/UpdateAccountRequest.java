package com.groupSWP.centralkitchenplatform.dto.auth;

import lombok.Data;

@Data
public class UpdateAccountRequest {
    private String fullName;
    private String email;

    // Đặt mật khẩu mới (có thể để trống nếu Admin chỉ muốn sửa tên/email, không muốn đổi pass)
    private String newPassword;
}