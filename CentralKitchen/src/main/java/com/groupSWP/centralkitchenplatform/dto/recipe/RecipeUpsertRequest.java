package com.groupSWP.centralkitchenplatform.dto.recipe;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecipeUpsertRequest {
    private String productId;
    private List<Item> ingredients;

    @Data
    public static class Item {
        private String ingredientId;
        private BigDecimal amountNeeded;
    }
}