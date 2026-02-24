package com.groupSWP.centralkitchenplatform.dto.auth;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String username,
        String role,
        String message // CẦN THÊM DÒNG NÀY VÀO ĐÂY
) {}