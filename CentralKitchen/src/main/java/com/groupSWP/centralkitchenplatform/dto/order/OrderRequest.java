package com.groupSWP.centralkitchenplatform.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String storeId;         // Cửa hàng nào đặt?
    private String orderType;       // Gửi chữ "STANDARD"
    private String deliveryWindow;  // Gửi chữ "MORNING" hoặc "AFTERNOON"
    private String note;            // Ghi chú thêm
    private List<OrderItemRequest> items; // Danh sách các món đặt

    @Data
    public static class OrderItemRequest {
        private String productId;   // Mã món (Product)
        private int quantity;       // Số lượng đặt
    }
}