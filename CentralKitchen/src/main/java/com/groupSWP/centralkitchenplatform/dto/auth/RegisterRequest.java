package com.groupSWP.centralkitchenplatform.dto.auth;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String fullName;
    private SystemRole role; // Nhận trực tiếp Enum (ADMIN, MANAGER...)
}