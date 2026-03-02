package com.groupSWP.centralkitchenplatform.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String storeId;         // Backend tự moi từ Token đè lên
    private String note;            // Ghi chú thêm (VD: Giao cẩn thận)
    private List<OrderItemRequest> items; // Danh sách các món đặt

    @Data
    public static class OrderItemRequest {
        private String productId;   // Mã món (Product)
        private int quantity;       // Số lượng đặt
    }
}