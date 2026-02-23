package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.formula.FormulaRequestDTO;
import com.groupSWP.centralkitchenplatform.dto.formula.FormulaResponseDTO;
import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.*;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.exception.NotFoundException;
import com.groupSWP.centralkitchenplatform.repositories.FormulaRepository;
import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaService {
    private final FormulaRepository formulaRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;

    public void saveFormulas(Product product, List<ProductRequest.Formula> ingredientRequests) {
        if (ingredientRequests == null || ingredientRequests.isEmpty()) return;

        List<Formula> formulaList = ingredientRequests.stream().map(item -> {
            Ingredient ingredient = ingredientRepository.findById(item.getIngredientId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nguyên liệu: " + item.getIngredientId()));

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

    public List<FormulaResponseDTO> getByProductId(String productId) {
        return formulaRepository.findByProduct_ProductId(productId)
                .stream()
                .map(f -> {
                    FormulaResponseDTO dto = new FormulaResponseDTO();
                    dto.setIngredientId(f.getIngredient().getIngredientId());
                    dto.setIngredientName(f.getIngredient().getName());
                    dto.setUnit(f.getIngredient().getUnit());
                    dto.setAmountNeeded(f.getAmountNeeded());
                    dto.setUnitCost(f.getIngredient().getUnitCost());

                    BigDecimal amount = f.getAmountNeeded();
                    BigDecimal cost = f.getIngredient().getUnitCost();
                    if (amount != null && cost != null) dto.setTotalCost(amount.multiply(cost));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void overwriteFormula(FormulaRequestDTO request) {
        // ✅ tìm PRODUCT (message đúng là sản phẩm)
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm: " + request.getProductId()));

        // ✅ xoá công thức cũ
        formulaRepository.deleteByProduct_ProductId(product.getProductId());

        // ✅ tạo công thức mới
        List<Formula> formulaList = request.getIngredients().stream().map(item -> {
            Ingredient ingredient = ingredientRepository.findById(item.getIngredientId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nguyên liệu: " + item.getIngredientId()));

            Formula formula = new Formula();
            formula.setId(new FormulaKey(product.getProductId(), ingredient.getIngredientId()));
            formula.setProduct(product);
            formula.setIngredient(ingredient);
            formula.setAmountNeeded(item.getAmountNeeded());
            return formula;
        }).collect(Collectors.toList());

        formulaRepository.saveAll(formulaList);
    }
}
