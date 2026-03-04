package com.groupSWP.centralkitchenplatform.service.system;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.repositories.system.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Cacheable(value = "systemConfigs", key = "#configKey")
    public String getConfigValue(String configKey, String defaultValue) {
        log.info("⚡ Chọc xuống DB lấy config: {} (Nếu thấy dòng này nghĩa là chưa có Cache RAM)", configKey);
        return systemConfigRepository.findById(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public LocalTime getLocalTimeConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
    }

    public BigDecimal getBigDecimalConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        return new BigDecimal(value);
    }

    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @CacheEvict(value = "systemConfigs", key = "#configKey")
    public SystemConfig updateConfig(String configKey, String configValue, String description, SystemUser updatedBy) {

        // 🛠️ ĐÃ SỬA CHỖ NÀY: Dùng orElseGet và new object thay vì builder
        SystemConfig config = systemConfigRepository.findById(configKey).orElseGet(() -> {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(configKey);
            return newConfig;
        });

        config.setConfigValue(configValue);
        if (description != null) config.setDescription(description);
        config.setUpdatedBy(updatedBy);

        log.info("Đã cập nhật cấu hình {} = {} và xóa Cache cũ!", configKey, configValue);
        return systemConfigRepository.save(config);
    }
}