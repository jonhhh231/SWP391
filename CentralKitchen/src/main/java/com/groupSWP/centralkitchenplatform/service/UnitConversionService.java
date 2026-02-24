package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.manager.ConversionRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.UnitConversion;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.UnitConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final UnitConversionRepository conversionRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public UnitConversion createConversion(ConversionRequest request) {
        // 1. Kiểm tra Nguyên liệu có tồn tại không?
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy nguyên liệu có ID: " + request.getIngredientId()));

        // 2. Validate Enum Unit (Chống nhập bậy)
        UnitType unitToConvert;
        try {
            unitToConvert = UnitType.valueOf(request.getUnitName());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Lỗi: Đơn vị '" + request.getUnitName() + "' không hợp lệ!");
        }

        // 3. Chặn trùng lặp (1 Nguyên liệu không thể có 2 công thức cho cùng 1 đơn vị)
        if (conversionRepository.existsByIngredientAndUnit(ingredient, unitToConvert)) {
            throw new RuntimeException("Lỗi: Quy đổi cho đơn vị '" + request.getUnitName() + "' đã tồn tại cho nguyên liệu này rồi!");
        }

        // 4. Tạo và Lưu
        UnitConversion conversion = UnitConversion.builder()
                .ingredient(ingredient)
                .unit(unitToConvert)
                .conversionFactor(request.getConversionFactor())
                .build();

        return conversionRepository.save(conversion);
    }

    /**
     * Hàm tính toán quy đổi
     * @param ingredient: Nguyên liệu (Gà)
     * @param inputUnit: Đơn vị nhập (Thùng)
     * @param quantity: Số lượng nhập (5)
     * @return Số lượng đã quy đổi ra đơn vị gốc (100)
     */
    public BigDecimal convertToBaseUnit(Ingredient ingredient, UnitType inputUnit, BigDecimal quantity) {
        // 1. Nếu đơn vị nhập vào trùng với đơn vị gốc (VD: Nhập KG, gốc là KG)
        if (inputUnit == ingredient.getUnit()) {
            return quantity;
        }

        // 2. Tìm công thức trong DB
        UnitConversion conversion = conversionRepository.findByIngredientAndUnit(ingredient, inputUnit)
                .orElseThrow(() -> new RuntimeException(
                        "Lỗi: Không tìm thấy công thức quy đổi từ '" + inputUnit +
                                "' sang '" + ingredient.getUnit() + "' cho nguyên liệu: " + ingredient.getName()));

        // 3. Tính toán: Số lượng * Hệ số (Factor)
        return quantity.multiply(conversion.getConversionFactor());
    }
}