package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse; // <--- Import DTO
import com.groupSWP.centralkitchenplatform.entities.product.Category;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.CategoryRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final FormulaService formulaService;
    private final CategoryRepository categoryRepository;
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // 1. Tìm Category từ ID gửi lên
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại!"));

        // 2. Tạo Product
        Product product = Product.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .category(category) // <--- Set Object Category vào đây
                .sellingPrice(request.getSellingPrice())
                .baseUnit(request.getBaseUnit())
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // 3. Xử lý Formula (giữ nguyên)
        formulaService.saveFormulas(savedProduct, request.getIngredients());

        // 4. Return DTO
        return mapToResponse(savedProduct);
    }

    // Hàm map mới
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .categoryName(product.getCategory().getName()) // Lấy tên từ bảng Category
                .categoryId(product.getCategory().getId())
                .sellingPrice(product.getSellingPrice())
                .baseUnit(product.getBaseUnit())
                .isActive(product.isActive())
                .build();
    }
}