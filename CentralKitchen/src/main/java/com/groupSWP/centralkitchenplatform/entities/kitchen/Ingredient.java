package com.groupSWP.centralkitchenplatform.entities.kitchen;

// [1] THÊM IMPORT NÀY
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
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
    // [2] SỬA DÒNG NÀY (Cũ là String -> Mới là UnitType + @Enumerated)
    @Enumerated(EnumType.STRING)
    private UnitType unit;
    private BigDecimal unitCost;
    private BigDecimal minThreshold;

    @OneToMany(mappedBy = "ingredient")
    private List<Formula> formulas;

    @OneToMany(mappedBy = "ingredient")
    private List<ImportItem> importItems;

    // [3] THÊM ĐOẠN NÀY VÀO CUỐI CÙNG (Để nối với bảng quy đổi mới)
    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitConversion> conversions;
}