package com.groupSWP.centralkitchenplatform.dto.order;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    // --- 4 TRƯỜNG CŨ CỦA SẾP (Giữ nguyên) ---
    private String orderId;
    private String status;
    private BigDecimal totalAmount;
    private String message;

    // --- BƠM THÊM CÁC TRƯỜNG MỚI CHO LỊCH SỬ ĐƠN ---
    private String storeId;
    private Order.OrderType orderType;
    private String note;
    private BigDecimal surcharge;

    // Danh sách các món trong đơn hàng này
    private List<OrderItemDto> items;

    @Data
    @Builder
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal priceAtOrder;
        private BigDecimal subTotal;
    }
}