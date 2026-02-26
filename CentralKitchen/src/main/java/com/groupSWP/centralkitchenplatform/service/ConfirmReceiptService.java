package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.product.Stock;
import com.groupSWP.centralkitchenplatform.entities.product.StockKey;
import com.groupSWP.centralkitchenplatform.repositories.OrderItemRepository;
import com.groupSWP.centralkitchenplatform.repositories.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.ShipmentRepository;
import com.groupSWP.centralkitchenplatform.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReceiptService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockRepository stockRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public ConfirmReceiptResponse confirmReceipt(String orderId, boolean updateStock, String note) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();

        // Idempotent: DONE rồi thì thôi (tránh cộng kho 2 lần)
        if (oldStatus == Order.OrderStatus.DONE) {
            log.info("ConfirmReceipt already confirmed order={} note={}", orderId, note);
            return ConfirmReceiptResponse.builder()
                    .orderId(orderId)
                    .oldStatus(oldStatus.name())
                    .newStatus(oldStatus.name())
                    .stockUpdated(false)
                    .shipmentCompleted(false)
                    .message("Đơn đã được xác nhận trước đó")
                    .build();
        }

        // Chỉ cho confirm khi đang giao
        if (oldStatus != Order.OrderStatus.SHIPPING) {
            throw new RuntimeException("Chỉ có thể xác nhận khi đơn đang SHIPPING");
        }

        // 1) Update order -> DONE
        order.setStatus(Order.OrderStatus.DONE);
        orderRepository.save(order);

        // 2) Optional: cộng kho
        boolean stockUpdated = false;
        if (updateStock) {
            // CẦN: orderItemRepository có method findByOrder_OrderId(String)
            List<OrderItem> items = orderItemRepository.findByOrder_OrderId(orderId);

            for (OrderItem item : items) {
                String storeId = order.getStore().getStoreId();
                String productId = item.getProduct().getProductId();

                StockKey key = new StockKey(storeId, productId);

                Stock stock = stockRepository.findById(key).orElseGet(() -> {
                    Stock s = new Stock();
                    s.setId(key);
                    s.setQuantity(0);
                    return s;
                });

                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stockRepository.save(stock);
            }
            stockUpdated = true;
        }

        // 3) Optional: hoàn tất shipment nếu mọi order trong shipment DONE
        boolean shipmentCompleted = false;
        if (order.getShipment() != null) {
            String shipmentId = order.getShipment().getShipmentId();

            boolean stillHasNotDone =
                    orderRepository.existsByShipment_ShipmentIdAndStatusNot(shipmentId, Order.OrderStatus.DONE);

            if (!stillHasNotDone) {
                // ⚠️ Nếu ShipmentRepository của bạn là <Shipment, UUID> thì sẽ lỗi.
                //    Bạn cần ShipmentRepository extends JpaRepository<Shipment, String>
                Shipment shipment = shipmentRepository.findById(shipmentId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy shipment: " + shipmentId));

                shipment.setStatus(Shipment.ShipmentStatus.COMPLETED);
                shipmentRepository.save(shipment);
                shipmentCompleted = true;
            }
        }

        log.info("ConfirmReceipt order={} old={} new={} stockUpdated={} shipmentCompleted={} note={}",
                orderId, oldStatus, Order.OrderStatus.DONE, stockUpdated, shipmentCompleted, note);

        return ConfirmReceiptResponse.builder()
                .orderId(orderId)
                .oldStatus(oldStatus.name())
                .newStatus(Order.OrderStatus.DONE.name())
                .stockUpdated(stockUpdated)
                .shipmentCompleted(shipmentCompleted)
                .message("Xác nhận nhập kho thành công")
                .build();
    }
}