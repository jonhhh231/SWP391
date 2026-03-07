package com.groupSWP.centralkitchenplatform.dto.auth;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;

public record RegisterRequest(
        String username,
        String password,
        String fullName,
        SystemUser.SystemRole role,
        String email, //
        String storeId
) {}