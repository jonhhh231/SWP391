package com.groupSWP.centralkitchenplatform.controllers.admin;

import com.groupSWP.centralkitchenplatform.dto.analytics.DashboardSummary;
import com.groupSWP.centralkitchenplatform.service.admin.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller cung cấp các API thống kê và phân tích dữ liệu (Analytics) dành cho cấp Quản lý.
 */
@Slf4j
@RestController
@RequestMapping("/api/manager/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // 🌟 BẢO MẬT: Khóa chặt cho cấp Quản lý
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * API Lấy dữ liệu tổng hợp cho trang chủ Dashboard (Hỗ trợ lọc theo ngày).
     * <p>
     * Truy xuất dữ liệu "sống" 100% từ Database. Hỗ trợ Frontend truyền startDate và endDate
     * để xem theo tuần, tháng, quý, năm.
     * </p>
     *
     * @param startDate Ngày bắt đầu (Định dạng: YYYY-MM-DD)
     * @param endDate   Ngày kết thúc (Định dạng: YYYY-MM-DD)
     * @return Phản hồi HTTP 200 chứa đối tượng {@link DashboardSummary}.
     */
    @GetMapping("/dashboard") // 🔥 FIX: Đổi tên /revenue thành /dashboard
    public ResponseEntity<DashboardSummary> getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 🌟 LOGIC MẶC ĐỊNH THỜI GIAN (Tránh lỗi nếu FE quên truyền)
        // Mặc định hiển thị dữ liệu 7 ngày gần nhất
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(6); // Lùi 6 ngày + hôm nay = 7 ngày
        }

        // Ép kiểu sang LocalDateTime (Lấy từ 00:00:00 ngày bắt đầu -> 23:59:59 ngày kết thúc)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        log.info("Manager đang xem thống kê xuất kho từ {} đến {}", startDateTime, endDateTime);

        DashboardSummary summary = analyticsService.getDashboardStats(startDateTime, endDateTime);
        return ResponseEntity.ok(summary);
    }
}