package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest; // Import đúng DTO mới đổi tên
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // API 1: Tạo sản phẩm mới (Đã có logic bên Service)
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    // API 1.2: Cập nhật sản phẩm
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // API 2: Lấy danh sách sản phẩm (Có phân trang & Lọc)
    // URL mẫu: GET /api/products?page=1&size=10&keyword=thịt&isActive=true
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "1") int page,           // Trang số mấy (Mặc định 1)
            @RequestParam(defaultValue = "10") int size,          // Lấy bao nhiêu dòng (Mặc định 10)
            @RequestParam(required = false) String keyword,       // Tìm theo tên
            @RequestParam(required = false) String category,      // Lọc theo loại
            @RequestParam(required = false) Boolean isActive,     // Lọc trạng thái (true/false)
            @RequestParam(required = false) BigDecimal minPrice,  // Giá thấp nhất
            @RequestParam(required = false) BigDecimal maxPrice,  // Giá cao nhất
            @RequestParam(defaultValue = "productName") String sortBy, // Sắp xếp theo cột nào (productName, sellingPrice...)
            @RequestParam(defaultValue = "asc") String sortDir    // Tăng dần (asc) hay giảm dần (desc)
    ) {
        // Gọi Service xử lý
        Page<ProductResponse> productPage = productService.getAllProducts(
                page, size, keyword, category, isActive, minPrice, maxPrice, sortBy, sortDir
        );

        // Đóng gói kết quả trả về JSON
        Map<String, Object> response = new HashMap<>();
        response.put("data", productPage.getContent());           // Danh sách sản phẩm
        response.put("currentPage", productPage.getNumber() + 1); // Trả về số trang (để Frontend dễ hiển thị)
        response.put("totalItems", productPage.getTotalElements()); // Tổng số lượng tìm thấy
        response.put("totalPages", productPage.getTotalPages());    // Tổng số trang

        return ResponseEntity.ok(response);
    }
}