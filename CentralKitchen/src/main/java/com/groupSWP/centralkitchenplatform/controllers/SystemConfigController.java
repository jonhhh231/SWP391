package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.config.SystemConfigRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.repositories.SystemUserRepository;
import com.groupSWP.centralkitchenplatform.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/manager/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final SystemUserRepository systemUserRepository; // Để móc thông tin người đang login

    // 1. Xem danh sách toàn bộ System Config
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    // 2. Cập nhật System Config (Ví dụ: Đổi giá trị URGENT_SURCHARGE)
    @PutMapping("/{configKey}")
    public ResponseEntity<SystemConfig> updateConfig(
            @PathVariable String configKey,
            @RequestBody SystemConfigRequest request,
            Principal principal
    ) {
        // Tìm xem ông Sếp nào đang login để ghi log (Auditing)
        SystemUser updatedBy = null;
        if (principal != null) {
            updatedBy = systemUserRepository.findByAccount_Username(principal.getName()).orElse(null);
        }

        SystemConfig updatedConfig = systemConfigService.updateConfig(
                configKey,
                request.configValue(),
                request.description(),
                updatedBy
        );

        return ResponseEntity.ok(updatedConfig);
    }
}