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

    /**
     * API Lấy toàn bộ tham số cấu hình hệ thống.
     * <p>Truy xuất danh sách chi tiết các giá trị cài đặt mặc định của hệ thống.</p>
     *
     * @return Phản hồi HTTP 200 chứa danh sách các đối tượng {@link SystemConfig}.
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    /**
     * API Lấy toàn bộ cấu hình dưới dạng cặp Key-Value.
     * <p>Phục vụ cho Frontend ánh xạ nhanh cấu hình vào bộ nhớ đệm (Cache/Store) để sử dụng.</p>
     *
     * @return Phản hồi HTTP 200 chứa Map các thông số cấu hình.
     */
    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getAllConfigsAsMap() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
        return ResponseEntity.ok(configMap);
    }

    /**
     * API Cập nhật một tham số cấu hình hệ thống.
     * <p>
     * Chỉnh sửa các giá trị vận hành như (Giờ đóng cửa, Phụ phí giao gấp...).
     * Có lưu vết tài khoản (Người thực hiện) thông qua Token.
     * </p>
     *
     * @param configKey Khóa định danh của tham số cấu hình.
     * @param request   Payload chứa giá trị mới và mô tả cập nhật.
     * @param principal Đối tượng bảo mật chứa danh tính người cập nhật.
     * @return Phản hồi HTTP 200 chứa thông số cấu hình sau khi sửa đổi.
     */
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
                configKey,
                request.configValue(),
                request.description(),
                updatedBy
        );
        return ResponseEntity.ok(updatedConfig);
    }
}