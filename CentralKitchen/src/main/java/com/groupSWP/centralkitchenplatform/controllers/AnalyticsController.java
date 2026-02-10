package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.analytics.ChartDataPoint;
import com.groupSWP.centralkitchenplatform.dto.analytics.DashboardSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/manager/analytics") // API dành riêng cho Manager
public class AnalyticsController {

    @GetMapping("/revenue")
    public ResponseEntity<DashboardSummary> getRevenueStats() {
        // --- 1. MOCK DATA (Dữ liệu giả lập cho biểu đồ) ---
        List<ChartDataPoint> fakeTrend = new ArrayList<>();

        // Giả lập dữ liệu 7 ngày gần nhất
        fakeTrend.add(new ChartDataPoint("2026-02-04", new BigDecimal("5000000"), 10L));
        fakeTrend.add(new ChartDataPoint("2026-02-05", new BigDecimal("7500000"), 15L));
        fakeTrend.add(new ChartDataPoint("2026-02-06", new BigDecimal("4200000"), 8L));
        fakeTrend.add(new ChartDataPoint("2026-02-07", new BigDecimal("8900000"), 20L));
        fakeTrend.add(new ChartDataPoint("2026-02-08", new BigDecimal("12000000"), 25L)); // Đỉnh điểm
        fakeTrend.add(new ChartDataPoint("2026-02-09", new BigDecimal("6000000"), 12L));
        fakeTrend.add(new ChartDataPoint("2026-02-10", new BigDecimal("9500000"), 18L)); // Hôm nay

        // --- 2. TỔNG HỢP (Số liệu thẻ bài) ---
        DashboardSummary summary = DashboardSummary.builder()
                .totalRevenueToday(new BigDecimal("9500000"))       // Giả bộ hôm nay bán 9.5tr
                .totalRevenueThisMonth(new BigDecimal("150000000"))   // Tháng này 150tr
                .totalOrdersToday(18L)                                // 18 đơn
                .revenueTrend(fakeTrend)                              // Gắn list biểu đồ vào
                .build();

        return ResponseEntity.ok(summary);
    }
}