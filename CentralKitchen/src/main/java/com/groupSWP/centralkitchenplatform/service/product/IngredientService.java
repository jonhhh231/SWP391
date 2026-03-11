package com.groupSWP.centralkitchenplatform.service.product; // Đổi lại package nếu bạn lưu ở thư mục khác

import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    // LẤY TẤT CẢ
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }

    // LẤY CHI TIẾT 1 CÁI
    public Ingredient getIngredientById(String id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu với ID: " + id));
    }

    // THÊM MỚI
    @Transactional
    public Ingredient createIngredient(Ingredient ingredient) {
        // Có thể thêm logic kiểm tra trùng tên ở đây trước khi save
        log.info("Đang tạo mới nguyên liệu...");
        return ingredientRepository.save(ingredient);
    }

    // CẬP NHẬT (UPDATE)
    @Transactional
    public Ingredient updateIngredient(String id, Ingredient ingredientDetails) {
        Ingredient existingIngredient = getIngredientById(id);

        // Cập nhật các trường cơ bản dựa theo Entity của bạn
        if (ingredientDetails.getName() != null) {
            existingIngredient.setName(ingredientDetails.getName());
        }
        if (ingredientDetails.getKitchenStock() != null) {
            existingIngredient.setKitchenStock(ingredientDetails.getKitchenStock());
        }
        if (ingredientDetails.getUnit() != null) {
            existingIngredient.setUnit(ingredientDetails.getUnit());
        }
        if (ingredientDetails.getUnitCost() != null) {
            existingIngredient.setUnitCost(ingredientDetails.getUnitCost());
        }
        if (ingredientDetails.getMinThreshold() != null) {
            existingIngredient.setMinThreshold(ingredientDetails.getMinThreshold());
        }

        log.info("Đã cập nhật nguyên liệu ID: {}", id);
        return ingredientRepository.save(existingIngredient);
    }

    // XÓA
    @Transactional
    public void deleteIngredient(String id) {
        Ingredient existingIngredient = getIngredientById(id);
        ingredientRepository.delete(existingIngredient);
        log.info("Đã xóa nguyên liệu ID: {}", id);
    }
}