package com.groupSWP.centralkitchenplatform.service.order;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCompleteDeliveryScheduler {

    private final OrderRepository orderRepository;

    // Chạy ngầm mỗi 15 phút (900000 ms) một lần. Bạn có thể chỉnh lại cho phù hợp.
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void autoConfirmDeliveredOrders() {
        // Mốc thời gian: Hiện tại trừ đi 6 tiếng
        LocalDateTime sixHoursAgo = LocalDateTime.now().minusHours(6);

        // Tìm tất cả đơn ĐANG GIAO mà thời gian bắt đầu giao đã trước mốc 6 tiếng
        List<Order> overdueOrders = orderRepository.findByStatusAndShippingStartTimeBefore(
                Order.OrderStatus.SHIPPING,
                sixHoursAgo
        );

        if (!overdueOrders.isEmpty()) {
            for (Order order : overdueOrders) {
                order.setStatus(Order.OrderStatus.DELIVERED);
                log.info("Hệ thống tự động chốt đơn {} vì quá 6 tiếng Store Manager không xác nhận.", order.getOrderId());
            }
            // Lưu lại toàn bộ cục này xuống DB
            orderRepository.saveAll(overdueOrders);
            log.info("Đã tự động xác nhận thành công {} đơn hàng.", overdueOrders.size());
        }
    }
}