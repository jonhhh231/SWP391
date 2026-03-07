package com.groupSWP.centralkitchenplatform.service.product;

import com.groupSWP.centralkitchenplatform.dto.recipe.RecipeResponse;
import com.groupSWP.centralkitchenplatform.dto.recipe.RecipeUpsertRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.FormulaKey;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.product.FormulaRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final FormulaRepository formulaRepo;
    private final ProductRepository productRepo;
    private final IngredientRepository ingredientRepo;

    @Transactional
    public void upsertRecipe(RecipeUpsertRequest req) {
        // Đã đổi sang IllegalArgumentException để báo lỗi 400 Bad Request
        if (req.getProductId() == null || req.getProductId().isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }
        if (req.getIngredients() == null || req.getIngredients().isEmpty()) {
            throw new IllegalArgumentException("Nguyên liệu không được rỗng");
        }

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + req.getProductId()));

        // Xoá công thức cũ (replace toàn bộ)
        formulaRepo.deleteByProduct_ProductId(req.getProductId());

        // 1. TẠO CÁI TÚI RỖNG ĐỂ GOM DATA
        List<Formula> formulasToSave = new ArrayList<>();

        for (RecipeUpsertRequest.Item item : req.getIngredients()) {
            if (item.getIngredientId() == null || item.getIngredientId().isBlank()) {
                throw new IllegalArgumentException("Mã nguyên liệu không được để trống");
            }
            if (item.getAmountNeeded() == null || item.getAmountNeeded().signum() <= 0) {
                throw new IllegalArgumentException("Số lượng cần phải lớn hơn 0");
            }

            Ingredient ing = ingredientRepo.findById(item.getIngredientId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nguyên liệu: " + item.getIngredientId()));

            Formula f = new Formula();
            f.setId(new FormulaKey(req.getProductId(), item.getIngredientId()));
            f.setProduct(product);
            f.setIngredient(ing);
            f.setAmountNeeded(item.getAmountNeeded());

            // 2. BỎ VÀO TÚI THAY VÌ LƯU LIỀN
            formulasToSave.add(f);
        }

        // 3. LƯU 1 LẦN DUY NHẤT VÀO DATABASE -> SIÊU MƯỢT!
        formulaRepo.saveAll(formulasToSave);
    }

    @Transactional(readOnly = true)
    public RecipeResponse getRecipeByProduct(String productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId));

        List<Formula> formulas = formulaRepo.findByProduct_ProductId(productId);

        return RecipeResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .ingredients(formulas.stream().map(f ->
                        RecipeResponse.Item.builder()
                                .ingredientId(f.getIngredient().getIngredientId())
                                .ingredientName(f.getIngredient().getName())
                                .unit(f.getIngredient().getUnit().name()) // LẤY TÊN ENUM RA (VD: "KG", "GRAM")
                                .amountNeeded(f.getAmountNeeded())
                                .build()                ).toList())
                .build();
    }

    @Transactional
    public void deleteRecipe(String productId) {
        formulaRepo.deleteByProduct_ProductId(productId);
    }
}