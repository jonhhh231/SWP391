package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse; // <--- Import DTO
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final FormulaService formulaService;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) { // <--- Đổi kiểu trả về
        // 1. Tạo và lưu Product
        Product product = Product.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .category(request.getCategory())
                .sellingPrice(request.getSellingPrice())
                .baseUnit(request.getBaseUnit())
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // 2. Lưu công thức (BOM)
        formulaService.saveFormulas(savedProduct, request.getIngredients());

        // 3. Convert sang DTO để trả về (Cắt đứt vòng lặp)
        return ProductResponse.builder()
                .productId(savedProduct.getProductId())
                .productName(savedProduct.getProductName())
                .category(savedProduct.getCategory())
                .sellingPrice(savedProduct.getSellingPrice())
                .baseUnit(savedProduct.getBaseUnit())
                .isActive(savedProduct.isActive())
                .build();
    }
}