package com.groupSWP.centralkitchenplatform.entities.kitchen;

import com.groupSWP.centralkitchenplatform.entities.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "formulas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Formula {
    @EmbeddedId
    private FormulaKey id;

    @Column(name = "amount_needed", nullable = false, precision = 38, scale = 2)
    private BigDecimal amountNeeded;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;
}