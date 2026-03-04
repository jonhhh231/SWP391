package com.groupSWP.centralkitchenplatform.dto.kitchen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WastageRequest {

    @NotBlank(message = "run_id (Mã mẻ nấu) không được để trống")
    private String runId;

    @NotNull(message = "Số lượng hao hụt không được để trống")
    @Positive(message = "Số lượng hao hụt (waste_qty) phải lớn hơn 0")
    private BigDecimal wasteQty;

    @NotBlank(message = "Bắt buộc phải nhập lý do hao hụt (VD: Cháy, đổ, sai công thức...)")
    private String reason;
}