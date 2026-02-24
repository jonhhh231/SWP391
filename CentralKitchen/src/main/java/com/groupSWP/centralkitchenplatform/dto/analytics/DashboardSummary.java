package com.groupSWP.centralkitchenplatform.dto.analytics;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummary {
    private BigDecimal totalRevenueToday;     // Doanh thu hôm nay
    private BigDecimal totalRevenueThisMonth; // Doanh thu tháng này
    private Long totalOrdersToday;            // Tổng đơn hôm nay
    private List<ChartDataPoint> revenueTrend;// Dữ liệu vẽ biểu đồ
}