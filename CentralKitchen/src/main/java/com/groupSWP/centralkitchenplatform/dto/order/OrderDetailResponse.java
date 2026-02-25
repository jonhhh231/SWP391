package com.groupSWP.centralkitchenplatform.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {
    private String orderId;
    private String storeId;
    private String orderType;
    private String status;
    private String deliveryWindow;
    private String note;
    private BigDecimal totalAmount;
    private BigDecimal surcharge; // Tiền phạt khẩn cấp (nếu có)
    private LocalDateTime createdAt;

    // Danh sách các món ăn bên trong đơn hàng
    private List<OrderItemDto> items;

    @Data
    @Builder
    public static class OrderItemDto {
        private String productId;
        private String productName; // Tên món để Cửa hàng dễ đọc
        private Integer quantity;
        private BigDecimal price;   // Giá lúc đặt
        private BigDecimal lineTotal; // Tổng tiền của món này (price * quantity)
    }
}