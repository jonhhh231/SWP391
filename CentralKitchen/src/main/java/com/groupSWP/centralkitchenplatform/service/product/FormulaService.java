package com.groupSWP.centralkitchenplatform.service.product;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.*;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.product.FormulaRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaService {
    private final FormulaRepository formulaRepository;
    private final IngredientRepository ingredientRepository;

    public void saveFormulas(Product product, List<ProductRequest.Formula> ingredientRequests) {
        if (ingredientRequests == null || ingredientRequests.isEmpty()) return;

        List<Formula> formulaList = ingredientRequests.stream().map(item -> {
            // Kiểm tra nguyên liệu có tồn tại không
            Ingredient ingredient = ingredientRepository.findById(item.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu: " + item.getIngredientId()));

            // Tạo Entity Formula
            Formula formula = new Formula();
            FormulaKey key = new FormulaKey(product.getProductId(), ingredient.getIngredientId());

            formula.setId(key);
            formula.setProduct(product);
            formula.setIngredient(ingredient);
            formula.setAmountNeeded(item.getAmountNeeded());

            return formula;
        }).collect(Collectors.toList());

        formulaRepository.saveAll(formulaList);
    }

    // 👇 THÊM HÀM NÀY
    @Transactional
    public void updateFormulas(Product product, List<ProductRequest.Formula> ingredientRequests) {
        // 1. Xóa sạch công thức cũ
        formulaRepository.deleteByProduct_ProductId(product.getProductId());

        // 2. Lưu lại cái mới (nếu có)
        if (ingredientRequests != null && !ingredientRequests.isEmpty()) {
            saveFormulas(product, ingredientRequests);
        }
    }
}