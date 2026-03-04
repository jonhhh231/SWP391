package com.groupSWP.centralkitchenplatform.service.order;

import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.product.Stock;
import com.groupSWP.centralkitchenplatform.entities.product.StockKey;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReceiptService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public ConfirmReceiptResponse confirmReceipt(String orderId, boolean updateStock, String note) {
        // 1. Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();

        // 2. Kiểm tra Idempotent (Đã hoàn thành thì không làm gì thêm)
        if (oldStatus == Order.OrderStatus.DONE) {
            log.info("Order {} already confirmed", orderId);
            return buildResponse(order, false, false, "Đơn đã được xác nhận trước đó");
        }

        // 3. Chỉ cho phép xác nhận khi đang SHIPPING
        if (oldStatus != Order.OrderStatus.SHIPPING) {
            throw new RuntimeException("Chỉ có thể xác nhận khi đơn đang SHIPPING. Trạng thái hiện tại: " + oldStatus);
        }

        // 4. Cập nhật Order -> DONE
        order.setStatus(Order.OrderStatus.DONE);
        orderRepository.save(order);

        // 5. Cập nhật Stock (Nếu updateStock = true)
        boolean stockUpdated = false;
        if (updateStock && order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                String storeId = order.getStore().getStoreId();
                String productId = item.getProduct().getProductId();
                StockKey key = new StockKey(storeId, productId);

                // Khởi tạo Stock mới nếu chưa có hoặc lấy từ DB
                Stock stock = stockRepository.findById(key).orElseGet(() -> {
                    Stock s = new Stock();
                    s.setId(key);
                    s.setQuantity(0);
                    s.setStore(order.getStore());   // Gán store từ order
                    s.setProduct(item.getProduct()); // Gán product từ item
                    return s;
                });

                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(stock);
            }
            stockUpdated = true;
        }

        // 6. Cập nhật Shipment (Nếu tất cả các đơn trong shipment đều đã DONE)
        boolean shipmentCompleted = updateShipmentStatusIfAllOrdersDone(order);

        log.info("ConfirmReceipt success: order={}, stockUpdated={}, shipmentCompleted={}",
                orderId, stockUpdated, shipmentCompleted);

        return buildResponse(order, stockUpdated, shipmentCompleted, "Xác nhận nhập kho thành công");
    }

    private boolean updateShipmentStatusIfAllOrdersDone(Order order) {
        if (order.getShipment() == null) return false;

        String shipmentId = order.getShipment().getShipmentId();

        // Kiểm tra xem còn đơn nào trong cùng Shipment mà chưa DONE không
        boolean stillHasNotDone = orderRepository.existsByShipment_ShipmentIdAndStatusNot(
                shipmentId, Order.OrderStatus.DONE);

        if (!stillHasNotDone) {
            Shipment shipment = order.getShipment();
            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
            shipmentRepository.save(shipment);
            return true;
        }
        return false;
    }

    private ConfirmReceiptResponse buildResponse(Order order, boolean stock, boolean ship, String msg) {
        return ConfirmReceiptResponse.builder()
                .orderId(order.getOrderId())
                .oldStatus(order.getStatus().name()) // Lưu ý: lúc này status đã là DONE nếu thành công
                .newStatus(order.getStatus().name())
                .stockUpdated(stock)
                .shipmentCompleted(ship)
                .message(msg)
                .build();
    }
}