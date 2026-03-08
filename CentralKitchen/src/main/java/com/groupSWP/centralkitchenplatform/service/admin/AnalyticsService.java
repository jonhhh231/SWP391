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

        // Mốc thời gian: Lấy từ đầu tháng (để tính doanh thu tháng)
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOf7DaysAgo = today.minusDays(6).atStartOfDay(); // 7 ngày gần nhất (tính cả hôm nay)

        // 1. CHỌC XUỐNG DB ĐÚNG 1 LẦN: Lấy tất cả đơn từ đầu tháng tới giờ (trừ đơn CANCELLED)
        List<Order> validOrdersThisMonth = orderRepository
                .findByCreatedAtGreaterThanEqualAndStatusNot(startOfMonth, Order.OrderStatus.CANCELLED);

        // 2. TÍNH TOÁN SỐ LIỆU "THÁNG NÀY"
        BigDecimal revenueThisMonth = calculateTotal(validOrdersThisMonth);

        // 3. LỌC RA SỐ LIỆU "HÔM NAY"
        List<Order> ordersToday = validOrdersThisMonth.stream()
                .filter(o -> !o.getCreatedAt().isBefore(startOfDay))
                .toList();

        long totalOrdersToday = ordersToday.size();
        BigDecimal revenueToday = calculateTotal(ordersToday);

        // 4. LỌC RA SỐ LIỆU "7 NGÀY QUA" VÀ VẼ BIỂU ĐỒ
        List<Order> last7DaysOrders = validOrdersThisMonth.stream()
                .filter(o -> !o.getCreatedAt().isBefore(startOf7DaysAgo))
                .toList();

        // Gom nhóm đơn hàng theo từng ngày (Ví dụ: Ngày 05 có 3 đơn, Ngày 06 có 10 đơn...)
        Map<LocalDate, List<Order>> ordersGroupedByDate = last7DaysOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<ChartDataPoint> trend = new ArrayList<>();
        // Vòng lặp đếm lùi từ 6 ngày trước cho tới hôm nay để vẽ biểu đồ từ Trái sang Phải
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            List<Order> dailyOrders = ordersGroupedByDate.getOrDefault(targetDate, new ArrayList<>());

            long dailyCount = dailyOrders.size();
            BigDecimal dailyRevenue = calculateTotal(dailyOrders);

            trend.add(new ChartDataPoint(targetDate.toString(), dailyRevenue, dailyCount));
        }

        // 5. ĐÓNG GÓI TRẢ VỀ CHO CONTROLLER
        return DashboardSummary.builder()
                .totalRevenueToday(revenueToday)
                .totalRevenueThisMonth(revenueThisMonth)
                .totalOrdersToday(totalOrdersToday)
                .revenueTrend(trend)
                .build();
    }

    // Hàm phụ trợ tính tổng tiền = (Tổng đơn) + (Phụ phí)
    private BigDecimal calculateTotal(List<Order> orders) {
        return orders.stream()
                .map(o -> {
                    BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal surcharge = o.getSurcharge() != null ? o.getSurcharge() : BigDecimal.ZERO;
                    return amount.add(surcharge);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
