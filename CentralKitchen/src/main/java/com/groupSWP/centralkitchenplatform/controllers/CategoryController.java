package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.entities.product.Category;
import com.groupSWP.centralkitchenplatform.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    // 1. Tạo danh mục mới
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    // 2. Lấy tất cả danh mục
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }
}