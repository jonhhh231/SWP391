package com.groupSWP.centralkitchenplatform.controllers.product;

import com.groupSWP.centralkitchenplatform.dto.recipe.RecipeResponse;
import com.groupSWP.centralkitchenplatform.dto.recipe.RecipeUpsertRequest;
import com.groupSWP.centralkitchenplatform.service.product.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/{productId}")
    public ResponseEntity<RecipeResponse> getRecipe(@PathVariable String productId) {
        return ResponseEntity.ok(recipeService.getRecipeByProduct(productId));
    }



    @PostMapping
    public ResponseEntity<String> upsertRecipe(@RequestBody RecipeUpsertRequest req) {
        recipeService.upsertRecipe(req);
        return ResponseEntity.ok("Lưu công thức thành công");
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteRecipe(@PathVariable String productId) {
        recipeService.deleteRecipe(productId);
        return ResponseEntity.ok("Xóa công thức thành công");
    }
}