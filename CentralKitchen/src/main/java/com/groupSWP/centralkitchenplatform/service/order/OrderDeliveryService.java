package com.groupSWP.centralkitchenplatform.service.order;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDeliveryService {

    private final OrderRepository orderRepository;

    // 1. KITCHEN MANAGER: Đánh dấu đang chuẩn bị
    @Transactional
    public void markAsPreparing(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        order.setStatus(Order.OrderStatus.PREPARING);
        orderRepository.save(order);
        log.info("Kitchen Manager đã cập nhật đơn {} sang PREPARING", orderId);
    }

    // 2. KITCHEN MANAGER: Đánh dấu đang giao (Bắt đầu đếm ngược 6 tiếng)
    @Transactional
    public void markAsShipping(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        order.setStatus(Order.OrderStatus.SHIPPING);
        order.setShippingStartTime(LocalDateTime.now()); // Chốt thời điểm bắt đầu giao
        orderRepository.save(order);

        // TODO: Gọi NotificationService ở đây để bắn thông báo (FCM, WebSocket, Email) cho Manager Store
        log.info("Kitchen Manager đã cập nhật đơn {} sang SHIPPING. Bắt đầu đếm ngược 6 tiếng. Đã gửi thông báo cho Cửa hàng!", orderId);
    }

    // 3. STORE MANAGER: Xác nhận đã nhận hàng
    @Transactional
    public void confirmReceipt(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        if (order.getStatus() != Order.OrderStatus.SHIPPING) {
            throw new RuntimeException("Đơn hàng này chưa được giao, không thể xác nhận!");
        }

        order.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(order);
        log.info("Store Manager đã xác nhận nhận thành công đơn {}", orderId);
    }
}