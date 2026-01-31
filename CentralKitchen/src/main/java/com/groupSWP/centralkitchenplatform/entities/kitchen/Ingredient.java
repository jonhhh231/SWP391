package com.groupSWP.centralkitchenplatform.entities.kitchen;

import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "ingredients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Ingredient {
    @Id
    private String ingredientId;
    private String name;
    private BigDecimal kitchenStock;
    private String unit;
    private BigDecimal unitCost;
    private BigDecimal minThreshold;

    @OneToMany(mappedBy = "ingredient")
    private List<Formula> formulas;

    @OneToMany(mappedBy = "ingredient")
    private List<ImportItem> importItems;
}