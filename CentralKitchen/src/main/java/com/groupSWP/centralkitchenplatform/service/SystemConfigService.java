package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.repositories.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    // ==========================================
    // 1. HÀM CORE: LẤY VALUE TỪ DB HOẶC CACHE RAM
    // ==========================================
    @Cacheable(value = "systemConfigs", key = "#configKey")
    public String getConfigValue(String configKey, String defaultValue) {
        log.info("⚡ Chọc xuống DB lấy config: {} (Nếu thấy dòng này nghĩa là chưa có Cache RAM)", configKey);
        // Lưu ý: Sếp đang xài findByConfigKey trả về Optional trong Repository
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    // ==========================================
    // 2. CÁC HÀM HELPER ÉP KIỂU SẴN (Cho OrderService xài)
    // ==========================================

    // Hàm lấy Tiền (VD: Phụ phí 100k)
    public BigDecimal getBigDecimalConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            log.error("Lỗi parse BigDecimal cho config {}. Value = {}", configKey, value);
            return new BigDecimal(defaultValue);
        }
    }

    // Hàm lấy Giờ (VD: Giờ chốt đơn 10:30, 13:00)
    public LocalTime getLocalTimeConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            log.error("Lỗi parse LocalTime cho config {}. Value = {}", configKey, value);
            return LocalTime.parse(defaultValue);
        }
    }

    // ==========================================
    // 3. HÀM QUÉT CACHE (Dùng khi Admin cập nhật giá trị mới)
    // ==========================================
    @CacheEvict(value = "systemConfigs", key = "#configKey")
    public void clearCache(String configKey) {
        log.info("Đã quét sạch Cache của config: {}. Lần gọi tới sẽ tự chọc lại DB!", configKey);
    }
}