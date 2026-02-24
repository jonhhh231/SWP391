package com.groupSWP.centralkitchenplatform.dto.formula;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FormulaRequestDTO {

    @NotBlank(message = "Mã sản phẩm không được để trống")
    private String productId;

    @NotEmpty(message = "Danh sách nguyên liệu không được để trống")
    @Valid
    private List<Item> ingredients;

    @Data
    public static class Item {

        @NotBlank(message = "Mã nguyên liệu không được để trống")
        private String ingredientId;

        @NotNull(message = "Số lượng cần dùng không được để trống")
        @DecimalMin(value = "0.0001", message = "Số lượng cần dùng phải lớn hơn 0")
        private BigDecimal amountNeeded;
    }
}