package com.groupSWP.centralkitchenplatform.controllers.product;

import com.groupSWP.centralkitchenplatform.dto.product.IngredientRequest; // 🌟 Thêm import DTO
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.service.product.IngredientService;
import jakarta.validation.Valid; // 🌟 Thêm import Valid để bật khiên bảo vệ
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    // 1. Lấy danh sách (Ai có token cũng xem được - Đã cấu hình trong SecurityConfig)
    @GetMapping
    public ResponseEntity<List<Ingredient>> getAll() {
        return ResponseEntity.ok(ingredientService.getAllIngredients());
    }

    // 2. Lấy chi tiết 1 nguyên liệu
    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getById(@PathVariable String id) {
        return ResponseEntity.ok(ingredientService.getIngredientById(id));
    }

    // 3. Thêm mới (Chỉ ADMIN, MANAGER, KITCHEN_MANAGER)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN_MANAGER')")
    @PostMapping
    public ResponseEntity<Ingredient> create(@Valid @RequestBody IngredientRequest request) { // 🌟 Dùng DTO + @Valid
        Ingredient created = ingredientService.createIngredient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 4. Cập nhật (Chỉ ADMIN, MANAGER, KITCHEN_MANAGER)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<Ingredient> update(@PathVariable String id, @Valid @RequestBody IngredientRequest request) { // 🌟 Dùng DTO + @Valid
        Ingredient updated = ingredientService.updateIngredient(id, request);
        return ResponseEntity.ok(updated);
    }

    // 5. Xóa (Chỉ ADMIN, MANAGER, KITCHEN_MANAGER)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa nguyên liệu thành công!"));
    }
}