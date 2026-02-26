package com.groupSWP.centralkitchenplatform.dto.kitchen;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WastageRequest {

    @NotNull(message = "run_id không được để trống")
    private String runId;

    @NotNull
    @Positive(message = "Số lượng hao hụt phải lớn hơn 0")
    private BigDecimal wasteQty;

    private String reason;
}