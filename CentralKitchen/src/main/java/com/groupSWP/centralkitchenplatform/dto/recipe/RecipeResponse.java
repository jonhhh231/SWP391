package com.groupSWP.centralkitchenplatform.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RecipeResponse {
    private String productId;
    private String productName;
    private List<Item> ingredients;

    @Data
    @Builder
    public static class Item {
        private String ingredientId;
        private String ingredientName;
        private BigDecimal amountNeeded;
    }
}