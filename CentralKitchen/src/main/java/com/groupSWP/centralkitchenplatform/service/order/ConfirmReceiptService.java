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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId)); // Sửa thành IllegalArgumentException

        Order.OrderStatus oldStatus = order.getStatus();

        // 2. Kiểm tra Idempotent (Đã hoàn thành thì không làm gì thêm)
        if (oldStatus == Order.OrderStatus.DONE) {
            log.info("Order {} already confirmed", orderId);
            return buildResponse(order, oldStatus.name(), false, false, "Đơn đã được xác nhận trước đó");
        }

        // 3. Chỉ cho phép xác nhận khi đang SHIPPING
        if (oldStatus != Order.OrderStatus.SHIPPING) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận khi đơn đang SHIPPING. Trạng thái hiện tại: " + oldStatus);
        }

        // 4. Cập nhật Order -> DONE và lưu Ghi chú
        order.setStatus(Order.OrderStatus.DONE);
        if (note != null && !note.trim().isEmpty()) {
            order.setNote(note); // Vớt lại cái ghi chú FE gửi lên
        }
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
                    s.setQuantity(0); // Nếu DB dùng BigDecimal thì nhớ đổi thành BigDecimal.ZERO nhé
                    s.setStore(order.getStore());
                    s.setProduct(item.getProduct());
                    return s;
                });

                // Chú ý: Cứ giả định Quantity của bạn là Integer, nếu là BigDecimal thì dùng .add()
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

        // 7. Truyền đúng oldStatus.name() vào để FE hiển thị chuẩn
        return buildResponse(order, oldStatus.name(), stockUpdated, shipmentCompleted, "Xác nhận nhập kho thành công");
    }

    private boolean updateShipmentStatusIfAllOrdersDone(Order order) {
        if (order.getShipment() == null) return false;

        String shipmentId = order.getShipment().getShipmentId();

        // Kiểm tra xem còn đơn nào trong cùng Shipment mà chưa DONE không
        boolean stillHasNotDone = orderRepository.existsByShipment_ShipmentIdAndStatusNot(
                shipmentId, Order.OrderStatus.DONE);

        if (!stillHasNotDone) {
            Shipment shipment = order.getShipment();
            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED); // Giả định có enum DELIVERED
            shipmentRepository.save(shipment);
            return true;
        }
        return false;
    }

    // Sửa lại hàm này để nhận biến oldStatusString
    private ConfirmReceiptResponse buildResponse(Order order, String oldStatusString, boolean stock, boolean ship, String msg) {
        return ConfirmReceiptResponse.builder()
                .orderId(order.getOrderId())
                .oldStatus(oldStatusString) // Đã sửa lỗi hiển thị sai
                .newStatus(order.getStatus().name()) // Lúc này sẽ là DONE
                .stockUpdated(stock)
                .shipmentCompleted(ship)
                .message(msg)
                .build();
    }
}