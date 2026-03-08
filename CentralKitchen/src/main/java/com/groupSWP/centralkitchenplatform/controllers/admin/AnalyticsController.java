package com.groupSWP.centralkitchenplatform.controllers.admin;

import com.groupSWP.centralkitchenplatform.dto.analytics.DashboardSummary;
import com.groupSWP.centralkitchenplatform.service.admin.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/analytics") // API dành riêng cho Manager
@RequiredArgsConstructor // Tự động Inject Service
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue")
    public ResponseEntity<DashboardSummary> getRevenueStats() {
        // Dữ liệu "sống" 100% lấy từ Database, tính toán Real-time
        DashboardSummary summary = analyticsService.getDashboardStats();
        return ResponseEntity.ok(summary);
    }
}