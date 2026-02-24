package com.groupSWP.centralkitchenplatform.entities.kitchen;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data @NoArgsConstructor @AllArgsConstructor
public class FormulaKey implements Serializable {
    @Column(name = "product_id")
    private String productId;

    @Column(name = "ingredient_id")
    private String ingredientId;
}