package com.groupSWP.centralkitchenplatform.dto.analytics;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardSummary {
    // 1. CÁC CHỈ SỐ TỔNG QUAN (Có kèm so sánh % với kỳ trước)
    private ComparisonMetric totalExportValue;  // Tổng giá trị xuất kho
    private ComparisonMetric totalOrders;       // Tổng số đơn hàng
    private ComparisonMetric totalWastageValue; // Tổng thiệt hại (Hao hụt bếp + Đền bù giao hàng)

    // 2. DỮ LIỆU BIỂU ĐỒ TỔNG HỢP
    private List<ChartDataPoint> exportTrend;

    // 3. CHI TIẾT THEO SẢN PHẨM (Top List)
    private List<ProductReportDto> topExportedProducts; // Top món xuất kho nhiều nhất
    private List<ProductReportDto> topWastedProducts;   // Top món hao hụt/lỗi nhiều nhất
}