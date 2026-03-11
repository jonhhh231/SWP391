package com.groupSWP.centralkitchenplatform.service.product;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import com.groupSWP.centralkitchenplatform.entities.product.Category;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.product.CategoryRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.ProductRepository;
import com.groupSWP.centralkitchenplatform.specifications.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final FormulaService formulaService;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        // 🔥 0. KIỂM TRA TRÙNG LẶP MÃ SẢN PHẨM Ở ĐÂY NÈ SẾP
        if (request.getProductId() == null || request.getProductId().trim().isEmpty()) {
            throw new RuntimeException("Mã sản phẩm không được để trống!");
        }

        // Hàm existsById là tính năng có sẵn cực xịn của Spring Data JPA
        if (productRepository.existsById(request.getProductId())) {
            throw new RuntimeException("Mã sản phẩm '" + request.getProductId() + "' đã tồn tại trong hệ thống. Vui lòng nhập mã khác!");
        }

        // 1. Tìm Category từ ID (Giữ nguyên code cũ của sếp)
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại!"));

        // 2. Khởi tạo Entity Product
        Product product = Product.builder()
                .productId(request.getProductId()) // Yên tâm xài mã FE truyền lên vì đã check trùng rồi!
                .productName(request.getProductName())
                .category(category)
                .sellingPrice(request.getSellingPrice())
                .baseUnit(UnitType.valueOf(request.getBaseUnit().toUpperCase()))
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // 3. Lưu công thức (BOM)
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            formulaService.saveFormulas(savedProduct, request.getIngredients());
        }

        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        // 1. Lấy sản phẩm hiện tại
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));

        // 2. Cập nhật thông tin cơ bản nếu có truyền lên
        if (request != null) {
            if (request.getProductName() != null) existingProduct.setProductName(request.getProductName());
            if (request.getSellingPrice() != null) existingProduct.setSellingPrice(request.getSellingPrice());
            if (request.getBaseUnit() != null && !request.getBaseUnit().trim().isEmpty()) {
                existingProduct.setBaseUnit(UnitType.valueOf(request.getBaseUnit().toUpperCase()));
            }
            if (request.getIsActive() != null) existingProduct.setActive(request.getIsActive());

            // Xử lý Category
            if (request.getCategoryId() != null) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại: " + request.getCategoryId()));
                existingProduct.setCategory(category);
            }
        }

        // 👉 ĐIỂM FIX LỖI: Chuyển lệnh save() lên TRƯỚC khi update công thức
        Product updatedProduct = productRepository.save(existingProduct);

        // 👉 ĐIỂM FIX LỖI: Update công thức cho Product ĐÃ ĐƯỢC SAVE
        if (request != null && request.getIngredients() != null) {
            formulaService.updateFormulas(updatedProduct, request.getIngredients());
        }

        return mapToResponse(updatedProduct);
    }

    @Transactional
    public String toggleProductStatus(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));

        // Đảo ngược trạng thái: Đang true (mở bán) -> false (ngừng bán) và ngược lại
        product.setActive(!product.isActive());
        productRepository.save(product);

        // Trả về câu thông báo cho FE mừng
        return product.isActive() ?
                "Đã MỞ BÁN lại sản phẩm: " + product.getProductName() :
                "Đã NGỪNG KINH DOANH sản phẩm: " + product.getProductName();
    }

    public Page<ProductResponse> getAllProducts(
            int page, int size,
            String keyword, String category,
            Boolean isActive,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        Specification<Product> spec = ProductSpecification.filterProducts(
                keyword, category, isActive, minPrice, maxPrice
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(this::mapToResponse);
    }

    // --- HÀM HELPER CHUYỂN ĐỔI ENTITY SANG DTO ---
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Chưa phân loại")
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .sellingPrice(product.getSellingPrice())
                .baseUnit(product.getBaseUnit() != null ? product.getBaseUnit().name() : null)
                .isActive(product.isActive())
                // .imageUrl(product.getImageUrl()) // Bạn có thể xóa dòng này nếu DTO cũng đã bỏ field imageUrl
                .build();
    }
}