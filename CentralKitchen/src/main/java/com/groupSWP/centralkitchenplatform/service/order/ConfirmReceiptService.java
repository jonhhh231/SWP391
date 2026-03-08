package com.groupSWP.centralkitchenplatform.service.order;

import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.product.Stock;
import com.groupSWP.centralkitchenplatform.entities.product.StockKey;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderItemRepository;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

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
            throw new IllegalArgumentException("Chỉ có thể xác nhận khi đơn đang SHIPPING. Trạng thái hiện tại: " + oldStatus.name());
        }

        // 1) Update order -> DONE
        order.setStatus(Order.OrderStatus.DONE);
        if (note != null) order.setNote(note);
        orderRepository.save(order);

        // 2) Optional: cộng kho (Đã Tối Ưu Performance)
        boolean stockUpdated = false;
        if (updateStock) {
            List<OrderItem> items = orderItemRepository.findByOrder_OrderId(orderId);
            String storeId = order.getStore().getStoreId();

            // TỐI ƯU READ: Gom tất cả Khóa (Keys) của các món hàng lại
            List<StockKey> stockKeys = items.stream()
                    .map(item -> new StockKey(storeId, item.getProduct().getProductId()))
                    .toList();

            // Bắn 1 câu lệnh SELECT duy nhất lấy lên toàn bộ kho, chuyển thành Map để tra cứu trên RAM
            Map<StockKey, Stock> existingStocksMap = stockRepository.findAllById(stockKeys)
                    .stream()
                    .collect(Collectors.toMap(Stock::getId, s -> s));

            List<Stock> stocksToSave = new ArrayList<>();

            // Vòng lặp tính toán (Chỉ chạy trên RAM)
            for (OrderItem item : items) {
                StockKey key = new StockKey(storeId, item.getProduct().getProductId());

                // Tra cứu kho từ Map. Nếu chưa có thì trả về một object rỗng mới tạo
                Stock stock = existingStocksMap.getOrDefault(key, new Stock());

                // Nếu là kho mới tinh chưa từng tồn tại trong DB
                if (stock.getId() == null) {
                    stock.setId(key);
                    stock.setQuantity(0); // Khởi tạo số lượng bằng 0 trước khi cộng
                    stock.setStore(order.getStore());
                    stock.setProduct(item.getProduct());
                }

                // Cộng dồn số lượng hàng nhận vào kho
                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stocksToSave.add(stock);
            }

            // TỐI ƯU WRITE: Lưu tất cả xuống DB trong 1 lần duy nhất
            stockRepository.saveAll(stocksToSave);
            stockUpdated = true;
        }

        // 3) Optional: hoàn tất shipment nếu mọi order trong shipment DONE
        boolean shipmentCompleted = false;
        if (order.getShipment() != null) {
            String shipmentId = order.getShipment().getShipmentId();

            boolean stillHasNotDone =
                    orderRepository.existsByShipment_ShipmentIdAndStatusNot(shipmentId, Order.OrderStatus.DONE);

            if (!stillHasNotDone) {
                Shipment shipment = shipmentRepository.findById(shipmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipment: " + shipmentId));

                shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
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