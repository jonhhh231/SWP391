package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse;
import com.groupSWP.centralkitchenplatform.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // API 1: Tạo sản phẩm mới (Nhận JSON Data + File Ảnh)
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        return ResponseEntity.ok(productService.createProduct(request, image));
    }

    // API 1.2: Cập nhật sản phẩm (Nhận JSON Data + File Ảnh mới nếu có)
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @RequestPart(value = "product", required = false) ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        return ResponseEntity.ok(productService.updateProduct(id, request, image));
    }

    // API 1.3: XÓA MỀM (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Đã ẩn sản phẩm thành công (Soft Delete)!");
    }

    // API 2: Lấy danh sách sản phẩm (Có phân trang & Lọc)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<ProductResponse> productPage = productService.getAllProducts(
                page, size, keyword, category, isActive, minPrice, maxPrice, sortBy, sortDir
        );

        Map<String, Object> response = new HashMap<>();
        response.put("data", productPage.getContent());
        response.put("currentPage", productPage.getNumber() + 1);
        response.put("totalItems", productPage.getTotalElements());
        response.put("totalPages", productPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}