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
    // 🌟 HÀM CHÍNH: LẤY BÁO CÁO DASHBOARD THEO KHOẢNG THỜI GIAN ĐỘNG
    // =======================================================================
    public DashboardSummary getDashboardStats(LocalDateTime startDate, LocalDateTime endDate) {

        // 1. TÍNH TOÁN KHOẢNG THỜI GIAN KỲ TRƯỚC (Để so sánh)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime previousStartDate = startDate.minusDays(daysBetween + 1);
        LocalDateTime previousEndDate = startDate.minusNanos(1);

        log.info("📊 Đang xuất báo cáo từ {} đến {} (So sánh với {} đến {})",
                startDate.toLocalDate(), endDate.toLocalDate(),
                previousStartDate.toLocalDate(), previousEndDate.toLocalDate());

        // 2. LẤY DỮ LIỆU ĐƠN HÀNG (Kỳ này & Kỳ trước)
        List<Order> currentOrders = orderRepository.findValidOrdersBetweenDates(startDate, endDate);
        List<Order> previousOrders = orderRepository.findValidOrdersBetweenDates(previousStartDate, previousEndDate);

        // 3. TÍNH TOÁN CÁC CHỈ SỐ XUẤT KHO (Đã đổi tên chuẩn nghiệp vụ)
        BigDecimal currentExportValue = calculateTotal(currentOrders);
        BigDecimal previousExportValue = calculateTotal(previousOrders);
        long currentOrderCount = currentOrders.size();
        long previousOrderCount = previousOrders.size();

        // 4. LẤY TOP 5 SẢN PHẨM (Xuất kho & Hao hụt) - Dùng Pageable để giới hạn 5 dòng
        Pageable top5 = PageRequest.of(0, 5);
        List<ProductReportDto> topExported = orderRepository.findTopExportedProducts(startDate, endDate, top5);
        List<ProductReportDto> topWasted = productionRunRepository.findTopWastedProductsInKitchen(startDate, endDate, top5);

        // Tính nhẩm tổng thiệt hại hao hụt từ danh sách Top
        BigDecimal currentWastageValue = topWasted.stream()
                .map(ProductReportDto::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. VẼ BIỂU ĐỒ (Nhóm đơn hàng theo ngày)
        List<ChartDataPoint> trend = buildChartTrend(currentOrders, startDate, endDate);

        // 6. ĐÓNG GÓI VÀO MÂM CỖ TRẢ VỀ CHO FRONTEND
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
    // 🛠️ CÁC HÀM PHỤ TRỢ XỬ LÝ SỐ LIỆU (HELPER METHODS)
    // =======================================================================

    // Hàm tính tổng tiền các đơn hàng
    private BigDecimal calculateTotal(List<Order> orders) {
        return orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Hàm vũ khí: Tính phần trăm tăng trưởng và gắn cờ xu hướng (UP/DOWN/FLAT)
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
                .growthPercentage(Math.abs(growth)) // Lấy số dương cho phần trăm
                .trend(trend)
                .build();
    }

    // Hàm nhào nặn dữ liệu biểu đồ
    private List<ChartDataPoint> buildChartTrend(List<Order> orders, LocalDateTime start, LocalDateTime end) {
        // Nhóm đơn hàng theo Ngày
        Map<LocalDate, List<Order>> ordersGroupedByDate = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<ChartDataPoint> trend = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDateLocal = end.toLocalDate();

        // Chạy vòng lặp từ ngày bắt đầu đến ngày kết thúc để đảm bảo không bị thiếu ngày nào (dù ngày đó không có đơn)
        while (!currentDate.isAfter(endDateLocal)) {
            List<Order> dailyOrders = ordersGroupedByDate.getOrDefault(currentDate, new ArrayList<>());
            long dailyCount = dailyOrders.size();
            BigDecimal dailyRevenue = calculateTotal(dailyOrders);

            trend.add(new ChartDataPoint(currentDate.toString(), dailyRevenue, dailyCount));
            currentDate = currentDate.plusDays(1);
        }
        return trend;
    }
}