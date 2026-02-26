package com.groupSWP.centralkitchenplatform.dto.kitchen;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WastageRequest {

    @NotNull(message = "run_id không được để trống")
    private String runId;

    @NotNull @Positive(message = "Số lượng hao hụt không được để trống ")
    private BigDecimal wasteQty;

    private String reason; // ghi chú tùy chọn
}