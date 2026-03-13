package com.groupSWP.centralkitchenplatform.service.product; // Đổi lại package nếu bạn lưu ở thư mục khác

import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.dto.product.IngredientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // ==========================================
    // 🌟 THÊM MỚI (Dùng DTO hứng data bảo mật)
    // ==========================================
    @Transactional
    public Ingredient createIngredient(IngredientRequest request) {
        log.info("Đang tạo mới nguyên liệu: {}", request.getName());

        // 🛑 Lỗ hổng A đã vá: Chặn giá trị <= 0 ở tầng Service cho chắc cốp
        if (request.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Lỗi: Đơn giá (unitCost) phải lớn hơn 0!");
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.getName());
        ingredient.setUnit(UnitType.valueOf(request.getUnit().toUpperCase()));
        ingredient.setUnitCost(request.getUnitCost());
        ingredient.setMinThreshold(request.getMinThreshold());

        // Mặc định lúc mới tạo thì tồn kho = 0 (Chỉ có nhập kho mới được tăng số này lên)
        ingredient.setKitchenStock(BigDecimal.ZERO);

        return ingredientRepository.save(ingredient);
    }

    // ==========================================
    // 🌟 CẬP NHẬT (Dùng DTO hứng data bảo mật)
    // ==========================================
    @Transactional
    public Ingredient updateIngredient(String id, IngredientRequest request) {
        Ingredient existingIngredient = getIngredientById(id);

        if (request != null) {
            if (request.getName() != null) existingIngredient.setName(request.getName());

            if (request.getUnit() != null) {
                existingIngredient.setUnit(UnitType.valueOf(request.getUnit().toUpperCase()));
            }

            if (request.getUnitCost() != null) {
                // 🛑 Gác cổng: Cập nhật giá cũng phải > 0
                if (request.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Lỗi: Đơn giá (unitCost) cập nhật phải lớn hơn 0!");
                }
                existingIngredient.setUnitCost(request.getUnitCost());
            }

            if (request.getMinThreshold() != null) {
                existingIngredient.setMinThreshold(request.getMinThreshold());
            }
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