package com.groupSWP.centralkitchenplatform.controllers.product;

import com.groupSWP.centralkitchenplatform.dto.manager.ConversionRequest;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.UnitConversion;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.service.product.UnitConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/manager/conversions")
@RequiredArgsConstructor
public class UnitConversionController {

    private final UnitConversionService conversionService;
    private final IngredientRepository ingredientRepository;

    // API Tạo Quy đổi mới
    // POST /api/manager/conversions
    @PostMapping
    public ResponseEntity<?> createConversion(@RequestBody ConversionRequest request) {
        try {
            UnitConversion result = conversionService.createConversion(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API Test tính toán
    // GET /api/manager/conversions/calculate?ingredientId=...&unit=...&quantity=...
    @GetMapping("/calculate")
    public ResponseEntity<?> calculateConversion(
            @RequestParam String ingredientId,
            @RequestParam String unit,
            @RequestParam BigDecimal quantity
    ) {
        try {
            // 1. Lấy thông tin nguyên liệu
            Ingredient ingredient = ingredientRepository.findById(ingredientId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu!"));

            // 2. Parse đơn vị từ String sang Enum
            UnitType inputUnit = UnitType.valueOf(unit);

            // 3. Gọi Service tính toán
            BigDecimal result = conversionService.convertToBaseUnit(ingredient, inputUnit, quantity);

            return ResponseEntity.ok(result); // Trả về số đã tính (VD: 100)
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================================
    // CÁC API MỚI THÊM: READ - UPDATE - DELETE
    // ==========================================================

    // API 1: Lấy danh sách quy đổi của 1 nguyên liệu (Dành cho Frontend làm Dropdown)
    // GET /api/manager/conversions/ingredient/{ingredientId}
    @GetMapping("/ingredient/{ingredientId}")
    public ResponseEntity<?> getConversionsByIngredient(@PathVariable String ingredientId) {
        return ResponseEntity.ok(conversionService.getConversionsByIngredient(ingredientId));
    }

    // API 2: Cập nhật hệ số quy đổi
    // PUT /api/manager/conversions/{id}?newFactor=25
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConversion(
            @PathVariable Long id,
            @RequestParam BigDecimal newFactor
    ) {
        try {
            UnitConversion result = conversionService.updateConversion(id, newFactor);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API 3: Xóa quy đổi
    // DELETE /api/manager/conversions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversion(@PathVariable Long id) {
        try {
            conversionService.deleteConversion(id);
            return ResponseEntity.ok("Đã xóa công thức quy đổi thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}