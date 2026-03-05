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
        // 🚨 TẠI SAO SỬA 1: Đổi `RuntimeException` thành `IllegalArgumentException`.
        // Lý do: Nếu dùng RuntimeException, Spring Boot sẽ quăng lỗi 500 (Server Error) đỏ lòm.
        // Đổi sang IllegalArgumentException thì cái file GlobalExceptionHandler tụi mình vừa viết
        // mới bắt được và ném về FE file JSON lỗi 400 siêu đẹp.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();

        // 2. Kiểm tra Idempotent (Đã hoàn thành thì không làm gì thêm)
        if (oldStatus == Order.OrderStatus.DONE) {
            log.info("Order {} already confirmed", orderId);
            // 🚨 TẠI SAO SỬA 2: Truyền thẳng `oldStatus.name()` vào hàm buildResponse.
            // Code cũ của bạn không truyền, nên FE sẽ không biết trạng thái cũ là gì.
            return buildResponse(order, oldStatus.name(), false, false, "Đơn đã được xác nhận trước đó");
        }

        // 3. Chỉ cho phép xác nhận khi đang SHIPPING
        if (oldStatus != Order.OrderStatus.SHIPPING) {
            // 🚨 TẠI SAO SỬA 1 (Lặp lại): Đổi để bắt lỗi đẹp cho FE.
            throw new IllegalArgumentException("Chỉ có thể xác nhận khi đơn đang SHIPPING. Trạng thái hiện tại: " + oldStatus);
        }

        // 4. Cập nhật Order -> DONE
        order.setStatus(Order.OrderStatus.DONE);

        // 🚨 TẠI SAO SỬA 3: Vớt lại biến `note`.
        // Lý do: Code cũ của bạn có nhận tham số `String note` nhưng lại không hề gọi `order.setNote()`.
        // Nếu không có đoạn này, ghi chú của cửa hàng gửi lên sẽ rơi vào hư vô, Database không lưu được.
        if (note != null && !note.trim().isEmpty()) {
            order.setNote(note);
        }
        orderRepository.save(order);

        // 5. Cập nhật Stock (Nếu updateStock = true)
        boolean stockUpdated = false;
        if (updateStock && order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                String storeId = order.getStore().getStoreId();
                String productId = item.getProduct().getProductId();
                StockKey key = new StockKey(storeId, productId);

                Stock stock = stockRepository.findById(key).orElseGet(() -> {
                    Stock s = new Stock();
                    s.setId(key);
                    // 💡 LƯU Ý NHỎ: Nếu biến Quantity của bạn là kiểu int/double thì gán 0 là đúng.
                    // Nhưng nếu DB bạn thiết kế kiểu BigDecimal thì chỗ này phải đổi thành BigDecimal.ZERO nhé!
                    s.setQuantity(0);
                    s.setStore(order.getStore());
                    s.setProduct(item.getProduct());
                    return s;
                });

                // 💡 LƯU Ý NHỎ: Tương tự, nếu dùng BigDecimal thì phải đổi thành:
                // stock.setQuantity(stock.getQuantity().add(item.getQuantity()));
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

        // 🚨 TẠI SAO SỬA 4: Bắt buộc phải truyền `oldStatus.name()` vào hàm buildResponse.
        return buildResponse(order, oldStatus.name(), stockUpdated, shipmentCompleted, "Xác nhận nhập kho thành công");
    }

    private boolean updateShipmentStatusIfAllOrdersDone(Order order) {
        if (order.getShipment() == null) return false;

        String shipmentId = order.getShipment().getShipmentId();

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

    // 🚨 TẠI SAO SỬA 4 (Tiếp theo): Sửa lại tham số đầu vào của hàm này để nhận `oldStatusString`.
    // Lý do: Code cũ của bạn gọi `.oldStatus(order.getStatus().name())`. Mà ở Bước 4 tụi mình đã
    // lỡ set nó thành DONE rồi. Nên nếu dùng code cũ, FE sẽ nhận được câu báo lỗi ngớ ngẩn:
    // "Trạng thái cũ là DONE, trạng thái mới là DONE". Truyền biến cũ vào thì nó mới báo đúng là
    // "Từ SHIPPING chuyển sang DONE".
    private ConfirmReceiptResponse buildResponse(Order order, String oldStatusString, boolean stock, boolean ship, String msg) {
        return ConfirmReceiptResponse.builder()
                .orderId(order.getOrderId())
                .oldStatus(oldStatusString)
                .newStatus(order.getStatus().name())
                .stockUpdated(stock)
                .shipmentCompleted(ship)
                .message(msg)
                .build();
    }
}