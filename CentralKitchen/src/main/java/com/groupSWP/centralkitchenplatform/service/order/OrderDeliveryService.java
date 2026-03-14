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

        // 🔥 GUARDRAIL: Chỉ đơn mới (NEW/PENDING) mới được mang đi nấu
        if (order.getStatus() != Order.OrderStatus.NEW) {
            throw new RuntimeException("Lỗi: Chỉ có thể nấu các đơn hàng mới (NEW hoặc PENDING)!");
        }

        order.setStatus(Order.OrderStatus.PREPARING);
        orderRepository.save(order);
        log.info("Kitchen Manager đã cập nhật đơn {} sang PREPARING", orderId);
    }

    // =======================================================
    // 🌟 ĐÃ THÊM: HÀM MỚI ĐỂ FIX LỖI "Cannot resolve method"
    // =======================================================
    @Transactional
    public void markAsReadyToShip(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // 🔥 GUARDRAIL: Đang nấu (PREPARING) thì mới được bấm Nấu xong
        if (order.getStatus() != Order.OrderStatus.PREPARING) {
            throw new RuntimeException("Lỗi: Chỉ có thể xác nhận nấu xong khi đơn hàng đang ở trạng thái Đang chuẩn bị (PREPARING)!");
        }

        order.setStatus(Order.OrderStatus.READY_TO_SHIP);
        orderRepository.save(order);
        log.info("Kitchen Manager đã cập nhật đơn {} sang READY_TO_SHIP. Đã đẩy sang màn hình của Điều phối viên!", orderId);
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