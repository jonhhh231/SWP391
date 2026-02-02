package com.groupSWP.centralkitchenplatform.dto.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {
    // 1. Thông tin cơ bản của món
    private String productId;       // VD: PHO-BO-DB
    private String productName;     // VD: Phở Bò Đặc Biệt
    private String category;        // VD: Main Course
    private BigDecimal sellingPrice;// VD: 50000
    private String baseUnit;        // VD: Tô

    // 2. Danh sách nguyên liệu (BOM) đi kèm
    // Đây là cái khó nhất, dùng List để hứng mảng []
    private List<Formula> ingredients;

    // Class con để hứng từng nguyên liệu trong mảng
    @Data
    public static class Formula {
        private String ingredientId;    // VD: THIT-BO
        private BigDecimal amountNeeded;// VD: 0.1 (kg)
    }
}