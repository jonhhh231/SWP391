package com.groupSWP.centralkitchenplatform.dto.formula;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FormulaResponseDTO {
    private String ingredientId;
    private String ingredientName;
    private String unit;
    private BigDecimal amountNeeded;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
}