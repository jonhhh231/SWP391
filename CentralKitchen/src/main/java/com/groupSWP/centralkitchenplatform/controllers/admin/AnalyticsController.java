package com.groupSWP.centralkitchenplatform.controllers.admin;

import com.groupSWP.centralkitchenplatform.dto.analytics.DashboardSummary;
import com.groupSWP.centralkitchenplatform.service.admin.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp các API thống kê và phân tích dữ liệu (Analytics) dành cho cấp Quản lý.
 * <p>
 * Lớp này chịu trách nhiệm giao tiếp với Frontend để cung cấp các chỉ số kinh doanh
 * quan trọng (như doanh thu, số lượng đơn hàng, chi phí, tồn kho...) nhằm hiển thị lên
 * biểu đồ của trang chủ Dashboard.
 * </p>
 * <p><b>Phân quyền:</b> Nằm trong nhóm {@code /api/manager/**}, các endpoint tại đây
 * chỉ cho phép người dùng có quyền {@code MANAGER} hoặc {@code ADMIN} truy cập.</p>
 */
@RestController
@RequestMapping("/api/manager/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * API Lấy dữ liệu tổng hợp cho trang chủ Dashboard.
     * <p>
     * Truy xuất dữ liệu "sống" 100% từ Database và tính toán theo thời gian thực (real-time).
     * Đảm bảo các chỉ số tài chính và vận hành luôn phản ánh đúng trạng thái hiện tại
     * của hệ thống mà không bị độ trễ.
     * </p>
     *
     * @return Phản hồi HTTP 200 (OK) chứa đối tượng {@link DashboardSummary} bao gồm
     * toàn bộ các chỉ số thống kê tổng quát.
     */
    @GetMapping("/revenue")
    public ResponseEntity<DashboardSummary> getRevenueStats() {
        DashboardSummary summary = analyticsService.getDashboardStats();
        return ResponseEntity.ok(summary);
    }
}