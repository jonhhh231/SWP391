package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType; // 👈 NHỚ IMPORT CÁI NÀY
import com.groupSWP.centralkitchenplatform.entities.product.Category;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.CategoryRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupSWP.centralkitchenplatform.specifications.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final FormulaService formulaService;
    private final CategoryRepository categoryRepository;

    /**
     * Tạo mới sản phẩm và công thức (BOM) đi kèm.
     * Transactional: Đảm bảo nếu lưu công thức lỗi thì roll back lại việc lưu sản phẩm.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // 1. Tìm Category từ ID gửi lên (Logic mới - Entity)
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại!"));

        // 2. Map request sang Entity và lưu Product
        Product product = Product.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .category(category) // Set Object Category vào đây
                .sellingPrice(request.getSellingPrice())
                // 👇 SỬA CHỖ NÀY: Chuyển String ("KG") -> Enum (UnitType.KG)
                // Lưu ý: Nếu request gửi bậy bạ (vd: "ABC") thì dòng này sẽ ném lỗi IllegalArgumentException
                .baseUnit(UnitType.valueOf(request.getBaseUnit()))
                .isActive(true) // Mặc định tạo mới là Active
                .build();

        Product savedProduct = productRepository.save(product);

        // 3. Lưu danh sách nguyên liệu (BOM) vào bảng Formula
        formulaService.saveFormulas(savedProduct, request.getIngredients());

        // 4. Return DTO
        return mapToResponse(savedProduct);
    }

    @Transactional
    public Product updateProduct(String id, ProductRequest request) {
        // 1. Tìm sản phẩm
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));

        // 2. Update thông tin cơ bản
        if (request.getProductName() != null) existingProduct.setProductName(request.getProductName());
        if (request.getSellingPrice() != null) existingProduct.setSellingPrice(request.getSellingPrice());
        if (request.getBaseUnit() != null) existingProduct.setBaseUnit(request.getBaseUnit());

        // 3. XỬ LÝ CATEGORY (Từ ID -> Object)
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category không tồn tại: " + request.getCategoryId()));
            existingProduct.setCategory(category);
        }

        // 4. Update công thức
        if (request.getIngredients() != null) {
            formulaService.updateFormulas(existingProduct, request.getIngredients());
        }

        if (request.getIsActive() != null) {
            existingProduct.setActive(request.getIsActive());
        }

        return productRepository.save(existingProduct);
    }

    /**
     * Xóa mềm (Soft Delete): Chỉ ẩn sản phẩm đi, không xóa khỏi DB.
     * Để bảo toàn lịch sử giao dịch.
     */
    @Transactional //
    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));

        // Chuyển trạng thái sang Inactive (Ẩn)
        // Lưu ý: Tùy Lombok sinh ra mà là setActive() hoặc setIsActive()
        product.setActive(false);

        productRepository.save(product);
    }

    /**
     * Lấy danh sách sản phẩm có Phân trang (Pagination) & Lọc (Filter)
     * Dùng cho cả Franchise Staff (đặt hàng) và Manager (quản lý).
     */
    public Page<ProductResponse> getAllProducts(
            int page, int size,
            String keyword, String category,
            Boolean isActive,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, String sortDir
    ) {
        // 1. Xử lý hướng sắp xếp (Giữ nguyên)
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // 2. Tạo đối tượng phân trang (Giữ nguyên)
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        // 3. TẠO SPECIFICATION (Thay đổi lớn ở đây)
        Specification<Product> spec = ProductSpecification.filterProducts(
                keyword, category, isActive, minPrice, maxPrice
        );

        // 4. Gọi Repository với Specification
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // 5. Convert sang DTO (Giữ nguyên)
        return productPage.map(this::mapToResponse);
    }

    // --- HÀM HELPER (Dùng chung) ---

    /**
     * Chuyển đổi Entity Product sang DTO ProductResponse.
     * Mục đích: Cắt đứt vòng lặp vô hạn (Infinite Recursion) của JPA khi trả về JSON.
     */
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                // Lấy thông tin từ Category Entity
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Chưa phân loại")
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .sellingPrice(product.getSellingPrice())
                // 👇 SỬA CHỖ NÀY: Chuyển Enum (UnitType.KG) -> String ("KG")
                .baseUnit(product.getBaseUnit().name())
                .isActive(product.isActive())
                .build();
    }
}