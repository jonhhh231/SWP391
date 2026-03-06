package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipmentManagementService {

    private final ShipmentRepository shipmentRepository;

    @Transactional
    public Shipment dispatchShipment(String shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến xe: " + shipmentId));

        // Chỉ xe nào đang PENDING mới được phép xuất bến
        if (shipment.getStatus() != Shipment.ShipmentStatus.PENDING) {
            throw new IllegalStateException("Xe tải đã xuất bến hoặc đã giao xong! Trạng thái hiện tại: " + shipment.getStatus());
        }

        // Đổi trạng thái chuyến xe thành Đang đi giao (SHIPPING)
        shipment.setStatus(Shipment.ShipmentStatus.SHIPPING);

        // Đổi trạng thái toàn bộ Đơn hàng trên xe thành SHIPPING để Cửa hàng có thể bấm nhận
        if (shipment.getOrders() != null) {
            for (Order order : shipment.getOrders()) {
                order.setStatus(Order.OrderStatus.SHIPPING);
            }
        }

        return shipmentRepository.save(shipment);
    }
}