package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    // Lấy danh sách đơn hàng của 1 cửa hàng cụ thể, sắp xếp cái nào mới đặt lên đầu
    // LƯU Ý: Nếu khóa chính của Store Sếp đặt là "id" thì đổi thành findByStore_Id
    List<Order> findByStore_StoreIdOrderByCreatedAtDesc(String storeId);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByStore_StoreId(String storeId);// Lấy tất cả đơn hàng của 1 cửa hàng
    boolean existsByShipment_ShipmentIdAndStatusNot(String shipmentId, Order.OrderStatus status);
    List<Order> findByStatusAndShipmentIsNull(Order.OrderStatus status);
}