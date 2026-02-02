package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final FormulaService formulaService; // Inject FormulaService vào đây

    @Transactional
    public Product createProduct(ProductRequest request) {
        // 1. Tạo và lưu Product trước
        Product product = Product.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .category(request.getCategory())
                .sellingPrice(request.getSellingPrice())
                .baseUnit(request.getBaseUnit())
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // 2. Gọi FormulaService để xử lý định mức (BOM)
        formulaService.saveFormulas(savedProduct, request.getIngredients());

        return savedProduct;
    }
}