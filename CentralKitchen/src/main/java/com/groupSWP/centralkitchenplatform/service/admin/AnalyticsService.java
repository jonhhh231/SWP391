package com.groupSWP.centralkitchenplatform.service.admin;

import com.groupSWP.centralkitchenplatform.dto.analytics.ChartDataPoint;
import com.groupSWP.centralkitchenplatform.dto.analytics.ComparisonMetric;
import com.groupSWP.centralkitchenplatform.dto.analytics.DashboardSummary;
import com.groupSWP.centralkitchenplatform.dto.analytics.ProductReportDto;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.repositories.inventory.ProductionRunRepository;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets; // 🌟 Thêm import này
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductionRunRepository productionRunRepository;

    // =======================================================================
    // 1. LẤY BÁO CÁO DASHBOARD THEO KHOẢNG THỜI GIAN ĐỘNG
    // =======================================================================
    /**
     * Lấy dữ liệu tổng hợp cho trang chủ Dashboard (Hỗ trợ lọc theo ngày).
     *
     * @param startDate Ngày bắt đầu.
     * @param endDate   Ngày kết thúc.
     * @return Đối tượng DashboardSummary chứa các chỉ số tổng quan và biểu đồ.
     */
    public DashboardSummary getDashboardStats(LocalDateTime startDate, LocalDateTime endDate) {

        // 1. Tính toán khoảng thời gian kỳ trước để đối chiếu tăng trưởng
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime previousStartDate = startDate.minusDays(daysBetween + 1);
        LocalDateTime previousEndDate = startDate.minusNanos(1);

        log.info("Đang xuất báo cáo từ {} đến {} (So sánh với {} đến {})",
                startDate.toLocalDate(), endDate.toLocalDate(),
                previousStartDate.toLocalDate(), previousEndDate.toLocalDate());

        // 2. Lấy dữ liệu đơn hàng (Kỳ này & Kỳ trước)
        List<Order> currentOrders = orderRepository.findValidOrdersBetweenDates(startDate, endDate);
        List<Order> previousOrders = orderRepository.findValidOrdersBetweenDates(previousStartDate, previousEndDate);

        // 3. Tính toán các chỉ số xuất kho
        BigDecimal currentExportValue = calculateTotal(currentOrders);
        BigDecimal previousExportValue = calculateTotal(previousOrders);
        long currentOrderCount = currentOrders.size();
        long previousOrderCount = previousOrders.size();

        // 4. Lấy Top 5 sản phẩm (Xuất kho & Hao hụt)
        Pageable top5 = PageRequest.of(0, 5);
        List<ProductReportDto> topExported = orderRepository.findTopExportedProducts(startDate, endDate, top5);
        List<ProductReportDto> topWasted = productionRunRepository.findTopWastedProductsInKitchen(startDate, endDate, top5);

        // Tính tổng tiền hao hụt từ danh sách Top
        BigDecimal currentWastageValue = topWasted.stream()
                .map(ProductReportDto::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Gom nhóm đơn hàng theo ngày để vẽ biểu đồ
        List<ChartDataPoint> trend = buildChartTrend(currentOrders, startDate, endDate);

        // 6. Xây dựng đối tượng phản hồi
        return DashboardSummary.builder()
                .totalExportValue(buildMetric(currentExportValue, previousExportValue))
                .totalOrders(buildMetric(BigDecimal.valueOf(currentOrderCount), BigDecimal.valueOf(previousOrderCount)))
                .totalWastageValue(buildMetric(currentWastageValue, BigDecimal.ZERO)) // Tạm thời để số 0 cho kỳ trước của Hao hụt
                .exportTrend(trend)
                .topExportedProducts(topExported)
                .topWastedProducts(topWasted)
                .build();
    }

    // =======================================================================
    // 2. CÁC HÀM PHỤ TRỢ XỬ LÝ SỐ LIỆU (HELPER METHODS)
    // =======================================================================

    private BigDecimal calculateTotal(List<Order> orders) {
        return orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ComparisonMetric buildMetric(BigDecimal current, BigDecimal previous) {
        double growth = 0.0;
        String trend = "FLAT";

        if (previous.compareTo(BigDecimal.ZERO) > 0) {
            growth = current.subtract(previous)
                    .divide(previous, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        } else if (current.compareTo(BigDecimal.ZERO) > 0) {
            growth = 100.0; // Từ 0 lên có số -> Tăng 100%
        }

        if (growth > 0) trend = "UP";
        else if (growth < 0) trend = "DOWN";

        return ComparisonMetric.builder()
                .currentValue(current)
                .previousValue(previous)
                .growthPercentage(Math.abs(growth))
                .trend(trend)
                .build();
    }

    private List<ChartDataPoint> buildChartTrend(List<Order> orders, LocalDateTime start, LocalDateTime end) {
        Map<LocalDate, List<Order>> ordersGroupedByDate = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<ChartDataPoint> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDateLocal = end.toLocalDate();

        // Chạy vòng lặp từ ngày bắt đầu đến kết thúc để điền đủ dữ liệu các ngày trống
        while (!currentDate.isAfter(endDateLocal)) {
            List<Order> dailyOrders = ordersGroupedByDate.getOrDefault(currentDate, new ArrayList<>());
            long dailyCount = dailyOrders.size();
            BigDecimal dailyRevenue = calculateTotal(dailyOrders);

            trend.add(new ChartDataPoint(currentDate.toString(), dailyRevenue, dailyCount));
            currentDate = currentDate.plusDays(1);
        }
        return trend;
    }

    // =======================================================================
    // 3. XUẤT FILE BÁO CÁO EXCEL (CSV)
    // =======================================================================
    /**
     * Xuất dữ liệu thống kê ra file định dạng CSV.
     *
     * @param startDate Ngày bắt đầu.
     * @param endDate   Ngày kết thúc.
     * @return Mảng byte chứa nội dung file CSV.
     */
    public byte[] exportDashboardToCsv(LocalDateTime startDate, LocalDateTime endDate) {
        DashboardSummary summary = getDashboardStats(startDate, endDate);

        StringBuilder csv = new StringBuilder();

        // Header (BOM để Excel hiển thị đúng tiếng Việt UTF-8)
        csv.append('\ufeff');
        csv.append("BAO CAO THONG KE HOAT DONG BEP TRUNG TAM\n");
        csv.append("Tu ngay:,").append(startDate.toLocalDate()).append(",Den ngay:,").append(endDate.toLocalDate()).append("\n\n");

        // Chỉ số tổng quan
        csv.append("1. CHI SO TONG QUAN\n");
        csv.append("Ten Chi So,Gia Tri Ky Nay,Gia Tri Ky Truoc,Tang Truong (%),Xu Huong\n");

        csv.append("Tong Gia tri Xuat kho,").append(summary.getTotalExportValue().getCurrentValue()).append(",")
                .append(summary.getTotalExportValue().getPreviousValue()).append(",")
                .append(summary.getTotalExportValue().getGrowthPercentage()).append("%,")
                .append(summary.getTotalExportValue().getTrend()).append("\n");

        csv.append("Tong So Don Hang,").append(summary.getTotalOrders().getCurrentValue()).append(",")
                .append(summary.getTotalOrders().getPreviousValue()).append(",")
                .append(summary.getTotalOrders().getGrowthPercentage()).append("%,")
                .append(summary.getTotalOrders().getTrend()).append("\n");

        csv.append("Tong Tien Hao Hut,").append(summary.getTotalWastageValue().getCurrentValue()).append(",")
                .append(summary.getTotalWastageValue().getPreviousValue()).append(",")
                .append(summary.getTotalWastageValue().getGrowthPercentage()).append("%,")
                .append(summary.getTotalWastageValue().getTrend()).append("\n\n");

        // Top 5 sản phẩm xuất kho
        csv.append("2. TOP 5 SAN PHAM XUAT KHO NHIEU NHAT\n");
        csv.append("Ma Mon,Ten Mon,Tong So Luong,Tong Tien (VNĐ)\n");
        for (ProductReportDto item : summary.getTopExportedProducts()) {
            csv.append(item.getProductId()).append(",")
                    .append(item.getProductName()).append(",")
                    .append(item.getTotalQuantity()).append(",")
                    .append(item.getTotalValue()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8); // 🌟 Đã gọt bớt đường dẫn dài
    }
}