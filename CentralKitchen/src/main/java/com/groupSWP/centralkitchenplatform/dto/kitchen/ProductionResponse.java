package com.groupSWP.centralkitchenplatform.dto.kitchen;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductionResponse {
    private String runId;
    private String productName;
    private BigDecimal plannedQty;
    private String status;
    private LocalDateTime productionDate;
}