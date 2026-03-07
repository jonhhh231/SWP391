package com.groupSWP.centralkitchenplatform.controllers.product;

import com.groupSWP.centralkitchenplatform.dto.recipe.RecipeResponse;
import com.groupSWP.centralkitchenplatform.dto.recipe.RecipeUpsertRequest;
import com.groupSWP.centralkitchenplatform.service.product.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ĐÃ SỬA: Dùng hasAnyAuthority để bao lô cả trường hợp có và không có chữ ROLE_
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER')")
    @PostMapping
    public ResponseEntity<String> upsertRecipe(@RequestBody RecipeUpsertRequest req) {
        recipeService.upsertRecipe(req);
        return ResponseEntity.ok("Lưu công thức thành công");
    }

    // ĐÃ SỬA: Dùng hasAnyAuthority
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER')")
    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteRecipe(@PathVariable String productId) {
        recipeService.deleteRecipe(productId);
        return ResponseEntity.ok("Xóa công thức thành công");
    }
}