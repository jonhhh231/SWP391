package com.groupSWP.centralkitchenplatform.service.admin;

import com.groupSWP.centralkitchenplatform.dto.analytics.ChartDataPoint;
import com.groupSWP.centralkitchenplatform.dto.analytics.DashboardSummary;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;

    public DashboardSummary getDashboardStats() {
        LocalDate today = LocalDate.now();

        // Mốc thời gian
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOf7DaysAgo = today.minusDays(6).atStartOfDay();

        // 1. Lấy tất cả đơn từ đầu tháng (Bỏ qua CANCELLED)
        List<Order> validOrdersThisMonth = orderRepository
                .findByCreatedAtGreaterThanEqualAndStatusNot(startOfMonth, Order.OrderStatus.CANCELLED);

        // 2. Doanh thu THÁNG NÀY
        BigDecimal revenueThisMonth = calculateTotal(validOrdersThisMonth);

        // 3. Doanh thu HÔM NAY
        List<Order> ordersToday = validOrdersThisMonth.stream()
                .filter(o -> !o.getCreatedAt().isBefore(startOfDay))
                .toList();

        long totalOrdersToday = ordersToday.size();
        BigDecimal revenueToday = calculateTotal(ordersToday);

        // 4. Biểu đồ 7 ngày
        List<Order> last7DaysOrders = validOrdersThisMonth.stream()
                .filter(o -> !o.getCreatedAt().isBefore(startOf7DaysAgo))
                .toList();

        Map<LocalDate, List<Order>> ordersGroupedByDate = last7DaysOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<ChartDataPoint> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            List<Order> dailyOrders = ordersGroupedByDate.getOrDefault(targetDate, new ArrayList<>());

            long dailyCount = dailyOrders.size();
            BigDecimal dailyRevenue = calculateTotal(dailyOrders);

            trend.add(new ChartDataPoint(targetDate.toString(), dailyRevenue, dailyCount));
        }

        return DashboardSummary.builder()
                .totalRevenueToday(revenueToday)
                .totalRevenueThisMonth(revenueThisMonth)
                .totalOrdersToday(totalOrdersToday)
                .revenueTrend(trend)
                .build();
    }

    /**
     * 🛠️ FIX BY MIMI:
     * Vì trong OrderService Sếp đã cộng Surcharge vào TotalAmount trước khi save,
     * nên ở đây mình chỉ cần lấy TotalAmount là đủ số liệu cuối cùng.
     */
    private BigDecimal calculateTotal(List<Order> orders) {
        return orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}