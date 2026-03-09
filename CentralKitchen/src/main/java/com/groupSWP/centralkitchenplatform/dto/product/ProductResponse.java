package com.groupSWP.centralkitchenplatform.dto.product;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProductResponse {
    private String productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal sellingPrice;
    private String baseUnit;
    private boolean isActive;
}