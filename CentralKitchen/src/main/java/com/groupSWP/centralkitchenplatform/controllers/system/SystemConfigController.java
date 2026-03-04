package com.groupSWP.centralkitchenplatform.controllers.system;

import com.groupSWP.centralkitchenplatform.dto.config.SystemConfigRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.repositories.auth.SystemUserRepository;
import com.groupSWP.centralkitchenplatform.service.system.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final SystemUserRepository systemUserRepository;

    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getAllConfigsAsMap() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
        return ResponseEntity.ok(configMap);
    }

    @PutMapping("/{configKey}")
    public ResponseEntity<SystemConfig> updateConfig(
            @PathVariable String configKey,
            @RequestBody SystemConfigRequest request,
            Principal principal
    ) {
        SystemUser updatedBy = null;
        if (principal != null) {
            updatedBy = systemUserRepository.findByAccount_Username(principal.getName()).orElse(null);
        }
        SystemConfig updatedConfig = systemConfigService.updateConfig(
                configKey, request.configValue(), request.description(), updatedBy
        );
        return ResponseEntity.ok(updatedConfig);
    }
}