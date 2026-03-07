package com.groupSWP.centralkitchenplatform.service.system;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.repositories.system.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
 import java.time.format.DateTimeParseException;
 import java.util.Arrays;
 import java.util.List;

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

        // ==========================================
        // 🛡️ LỚP BẢO VỆ (VALIDATION) BẮT ĐẦU Ở ĐÂY
        // ==========================================

        // 1. Chống chế Key rác (Tạm thời khóa cứng 4 Key cốt lõi này để an toàn)
        List<String> validKeys = Arrays.asList("OPEN_TIME", "URGENT_CUTOFF_TIME", "STANDARD_CUTOFF_TIME", "URGENT_SURCHARGE");
        if (!validKeys.contains(configKey)) {
            throw new RuntimeException("Không được phép tự tạo cấu hình mới! Chỉ hỗ trợ sửa các cấu hình hệ thống hiện có.");
        }

        // 2. Bắt lỗi Format (Định dạng) theo đúng chuẩn Sếp dùng bên dưới
        if (configKey.endsWith("_TIME")) {
            try {
                // Sếp đang xài pattern HH:mm ở hàm get, nên mình bắt validate y chang vậy
                LocalTime.parse(configValue, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Định dạng giờ sai! Vui lòng nhập chuẩn HH:mm (Ví dụ: 13:00, 08:30)");
            }
        }
        else if (configKey.endsWith("_SURCHARGE")) {
            try {
                BigDecimal money = new BigDecimal(configValue);
                if (money.compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("Tiền phụ phí không được là số âm!");
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Định dạng tiền sai! Vui lòng chỉ nhập các con số (Ví dụ: 100000)");
            }
        }
        // ==========================================
        // 🛡️ HẾT LỚP BẢO VỆ
        // ==========================================

        // 🛠️ ĐÃ SỬA CHỖ NÀY: Dùng orElseGet và new object thay vì builder
        SystemConfig config = systemConfigRepository.findById(configKey).orElseGet(() -> {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(configKey);
            return newConfig;
        });

        config.setConfigValue(configValue);
        if (description != null) config.setDescription(description);
        config.setUpdatedBy(updatedBy);

        log.info("🔥 Đã cập nhật cấu hình {} = {} và xóa Cache cũ!", configKey, configValue);
        return systemConfigRepository.save(config);
    }
}