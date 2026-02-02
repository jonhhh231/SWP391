package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.*;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final FormulaRepository formulaRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public Product createProduct(ProductRequest request) {
        // 1. Lưu Product
        Product product = new Product();
        product.setProductId(request.getProductId());
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setSellingPrice(request.getSellingPrice());
        product.setBaseUnit(request.getBaseUnit());
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        // 2. Lưu Formula
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            List<Formula> formulaList = new ArrayList<>();

            // Lưu ý: Dùng ProductRequest.Formula để không nhầm với Entity Formula
            for (ProductRequest.Formula item : request.getIngredients()) {

                Ingredient ing = ingredientRepository.findById(item.getIngredientId())
                        .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy nguyên liệu ID: " + item.getIngredientId()));

                // Đây là Entity Formula (bảng trong DB)
                Formula formula = new Formula();

                FormulaKey key = new FormulaKey(savedProduct.getProductId(), ing.getIngredientId());
                formula.setId(key);

                formula.setProduct(savedProduct);
                formula.setIngredient(ing);
                formula.setAmountNeeded(item.getAmountNeeded());

                formulaList.add(formula);
            }
            formulaRepository.saveAll(formulaList);
        }

        return savedProduct;
    }
}