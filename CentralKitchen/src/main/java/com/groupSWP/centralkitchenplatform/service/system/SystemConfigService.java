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
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Service quản lý Cấu hình hệ thống (System Configurations).
 * <p>
 * Cung cấp cơ chế lưu trữ và truy xuất các tham số vận hành chung (như giờ mở cửa, phụ phí...).
 * Tích hợp Cache RAM (Spring Cache) để tối ưu hóa hiệu suất truy vấn cấu hình liên tục.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * Lấy giá trị cấu hình theo Khóa (Config Key) - Có áp dụng Cache.
     */
    @Cacheable(value = "systemConfigs", key = "#configKey")
    public String getConfigValue(String configKey, String defaultValue) {
        log.info("⚡ Chọc xuống DB lấy config: {} (Nếu thấy dòng này nghĩa là chưa có Cache RAM)", configKey);
        return systemConfigRepository.findById(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Tiện ích: Lấy cấu hình dạng Giờ (LocalTime).
     */
    public LocalTime getLocalTimeConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Tiện ích: Lấy cấu hình dạng Tiền tệ/Số học (BigDecimal).
     */
    public BigDecimal getBigDecimalConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        return new BigDecimal(value);
    }

    /**
     * Lấy toàn bộ danh sách cấu hình.
     */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    /**
     * Cập nhật cấu hình hệ thống.
     * <p>Xóa Cache RAM hiện tại để hệ thống tự động tải lại dữ liệu mới ở lần gọi tiếp theo.</p>
     *
     * @param configKey   Khóa cấu hình.
     * @param configValue Giá trị cấu hình mới.
     * @param description Mô tả thay đổi.
     * @param updatedBy   Người thực hiện cập nhật.
     * @return Thực thể cấu hình sau cập nhật.
     */
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