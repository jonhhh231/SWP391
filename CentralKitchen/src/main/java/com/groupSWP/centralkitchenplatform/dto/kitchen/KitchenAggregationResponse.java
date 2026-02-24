package com.groupSWP.centralkitchenplatform.dto.kitchen;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class KitchenAggregationResponse {
    private String date;                 // yyyy-MM-dd
    private String deliveryWindow;       // MORNING/AFTERNOON/null
    private String status;               // NEW/...
    private List<ProductAgg> products;
    private List<IngredientAgg> ingredients; // null nếu includeIngredients=false

    @Data
    public static class ProductAgg {
        private String productId;
        private String productName;
        private Integer totalQty;
    }

    @Data
    public static class IngredientAgg {
        private String ingredientId;
        private String ingredientName;
        private String unit;
        private BigDecimal totalNeeded; // tổng lượng cần dùng
    }
}