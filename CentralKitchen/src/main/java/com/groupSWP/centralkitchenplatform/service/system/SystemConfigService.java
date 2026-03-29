package com.groupSWP.centralkitchenplatform.service.system;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.entities.notification.Notification; // 🔥 Thêm import
import com.groupSWP.centralkitchenplatform.repositories.system.SystemConfigRepository;
import com.groupSWP.centralkitchenplatform.service.notification.NotificationService; // 🔥 Thêm import
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
 * <p>
 * <b>Kiến trúc và Tầm quan trọng:</b> Lớp Service này đóng vai trò là "bộ não" điều phối các tham số
 * biến thiên của toàn bộ nền tảng Central Kitchen. Việc ứng dụng Spring Cache (Cacheable/CacheEvict)
 * giúp hệ thống giảm tải đáng kể cho cơ sở dữ liệu khi xử lý hàng ngàn yêu cầu truy vấn cấu hình mỗi giây
 * trong khung giờ cao điểm. Các tham số như khung giờ chốt đơn (Cutoff Time) hay phụ phí (Surcharge)
 * đều được thẩm định qua lớp bảo vệ Validation nghiêm ngặt, ngăn chặn triệt để các lỗi định dạng
 * có thể gây đứt gãy quy trình tính toán tự động. Sau mỗi lần cập nhật, hệ thống không chỉ
 * làm mới bộ nhớ đệm mà còn tự động kích hoạt luồng Broadcast Notification, đảm bảo sự
 * đồng bộ thông tin ngay lập tức giữa các bộ phận quản lý chi nhánh, kho vận và nhà bếp,
 * từ đó duy trì tính nhất quán và minh bạch cho toàn bộ chuỗi cung ứng lạnh của dự án.
 * </p>
 *
 * @author Đạt, Huy, Triển
 * @version 1.0
 * @since 2026-03-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final NotificationService notificationService; // 🔥 Tiêm NotificationService vào đây

    /**
     * Lấy giá trị cấu hình theo Khóa (Config Key) - Có áp dụng Cache.
     * <p>
     * Dữ liệu sẽ được ưu tiên lấy từ bộ nhớ đệm RAM. Nếu không có (Cache Miss),
     * hệ thống mới thực hiện truy vấn xuống Database.
     * </p>
     *
     * @param configKey    Khóa định danh của cấu hình (VD: OPEN_TIME).
     * @param defaultValue Giá trị trả về nếu không tìm thấy cấu hình trong DB.
     * @return Chuỗi giá trị cấu hình tương ứng.
     */
    @Cacheable(value = "systemConfigs", key = "#configKey")
    public String getConfigValue(String configKey, String defaultValue) {
        log.info("⚡ Chọc xuống DB lấy config: {} (Nếu thấy dòng này nghĩa là chưa có Cache RAM)", configKey);
        return systemConfigRepository.findById(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Tiện ích: Lấy cấu hình và chuyển đổi sang dạng Giờ (LocalTime).
     *
     * @param configKey    Khóa cấu hình cần tra cứu.
     * @param defaultValue Giá trị thời gian mặc định (định dạng HH:mm).
     * @return Đối tượng {@link LocalTime} sau khi đã phân tích chuỗi giá trị.
     */
    public LocalTime getLocalTimeConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Tiện ích: Lấy cấu hình dạng Tiền tệ/Số học (BigDecimal).
     *
     * @param configKey    Khóa cấu hình cần tra cứu.
     * @param defaultValue Giá trị số mặc định dưới dạng chuỗi.
     * @return Đối tượng {@link BigDecimal} tương ứng.
     */
    public BigDecimal getBigDecimalConfig(String configKey, String defaultValue) {
        String value = getConfigValue(configKey, defaultValue);
        return new BigDecimal(value);
    }

    /**
     * Lấy toàn bộ danh sách cấu hình hiện hành trong hệ thống.
     *
     * @return Danh sách các thực thể {@link SystemConfig}.
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
     * @throws RuntimeException Nếu khóa không hợp lệ hoặc giá trị mới sai định dạng (Giờ/Tiền).
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

        SystemConfig savedConfig = systemConfigRepository.save(config);

        // ==========================================================
        // 🔥 THÔNG BÁO: PHÁT LOA CHO CÁC BỘ PHẬN BIẾT CẤU HÌNH ĐÃ ĐỔI
        // ==========================================================
        notificationService.broadcastNotification(
                List.of("STORE_MANAGER", "KITCHEN_MANAGER", "COORDINATOR"), // Gửi cho 3 bên
                "⚙️ THAY ĐỔI CẤU HÌNH HỆ THỐNG",
                "Quản lý vừa cập nhật tham số [" + configKey + "] thành giá trị mới là: " + configValue + ". Vui lòng lưu ý để sắp xếp công việc!",
                Notification.NotificationType.INFO
        );

        return savedConfig;
    }
}