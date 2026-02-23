package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    // Lọc theo status + createdAt trong ngày
    List<Order> findByStatusAndCreatedAtBetween(
            Order.OrderStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    // Lọc thêm deliveryWindow là khung thời gian giao hàng (ca giao) mà đơn hàng muốn nhận / hệ thống xử lý + orderType list
    List<Order> findByStatusAndDeliveryWindowAndOrderTypeInAndCreatedAtBetween(
            Order.OrderStatus status,
            Order.DeliveryWindow deliveryWindow,
            List<Order.OrderType> orderTypes,
            LocalDateTime from,
            LocalDateTime to
    );
}