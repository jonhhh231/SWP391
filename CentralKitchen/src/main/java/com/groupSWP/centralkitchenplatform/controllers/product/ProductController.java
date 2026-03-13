package com.groupSWP.centralkitchenplatform.controllers.product;

import com.groupSWP.centralkitchenplatform.dto.product.ProductRequest;
import com.groupSWP.centralkitchenplatform.dto.product.ProductResponse;
import com.groupSWP.centralkitchenplatform.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller quản lý danh mục Sản phẩm (Product Management).
 * <p>
 * Lớp này cung cấp các API xử lý vòng đời của một sản phẩm thương mại (như món ăn, đồ uống,
 * combo) được cung cấp bởi Bếp trung tâm. Điểm nhấn của Controller này là hệ thống
 * truy vấn dữ liệu mạnh mẽ, hỗ trợ phân trang, lọc theo nhiều tiêu chí và sắp xếp động.
 * </p>
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // =======================================================
    // API 1: TẠO SẢN PHẨM MỚI
    // =======================================================
    /**
     * API Khởi tạo một sản phẩm mới.
     * <p>Nhận dữ liệu định dạng JSON thuần để tạo mới một mặt hàng trong hệ thống.</p>
     *
     * @param request Payload chứa thông tin sản phẩm (Tên, Giá, Danh mục, Hình ảnh...).
     * @return Phản hồi HTTP 200 chứa thông tin {@link ProductResponse} vừa được tạo thành công.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN_MANAGER')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    // =======================================================
    // API 1.2: CẬP NHẬT SẢN PHẨM
    // =======================================================
    /**
     * API Cập nhật thông tin chi tiết của sản phẩm.
     * <p>Dữ liệu JSON gửi lên sẽ ghi đè lên các thuộc tính hiện tại của sản phẩm dựa trên ID.</p>
     *
     * @param id      Mã định danh của sản phẩm cần thay đổi.
     * @param request Payload chứa dữ liệu cập nhật mới.
     * @return Phản hồi HTTP 200 chứa dữ liệu sản phẩm sau khi đã được cập nhật.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN_MANAGER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // =======================================================
    // API 1.3: NÚT GẠT TRẠNG THÁI (XÓA MỀM)
    // =======================================================
    /**
     * API Thay đổi trạng thái hoạt động của sản phẩm (Kích hoạt / Vô hiệu hóa).
     * <p>
     * Hệ thống áp dụng cơ chế "Xóa mềm" (Soft Delete). Thay vì xóa vật lý khỏi Database
     * gây mất dấu lịch sử đơn hàng, API này chỉ gạt trạng thái ẩn/hiện của sản phẩm.
     * Cửa hàng sẽ không thể nhìn thấy hoặc đặt mua các sản phẩm đã bị vô hiệu hóa.
     * </p>
     *
     * @param id Mã định danh của sản phẩm cần thay đổi trạng thái.
     * @return Phản hồi HTTP 200 kèm chuỗi thông báo trạng thái mới của sản phẩm.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN_MANAGER')")
    public ResponseEntity<String> toggleProductStatus(@PathVariable String id) {
        return ResponseEntity.ok(productService.toggleProductStatus(id));
    }

    // =======================================================
    // API 2: LẤY DANH SÁCH SẢN PHẨM (PHÂN TRANG & LỌC)
    // =======================================================
    /**
     * API Truy xuất danh sách Sản phẩm nâng cao (Pagination, Filtering, Sorting).
     * <p>
     * Cung cấp một bộ lọc toàn diện giúp Frontend dễ dàng xây dựng màn hình danh sách sản phẩm.
     * Kết quả trả về được gói gọn trong một cấu trúc JSON chuẩn, kèm theo các siêu dữ liệu
     * (metadata) để hỗ trợ phân trang trên giao diện.
     * </p>
     *
     * @param page      Số thứ tự trang cần lấy (Mặc định: 1).
     * @param size      Số lượng bản ghi trên mỗi trang (Mặc định: 10).
     * @param keyword   Từ khóa tìm kiếm theo tên sản phẩm (Tùy chọn).
     * @param category  Lọc theo tên hoặc mã danh mục (Tùy chọn).
     * @param isActive  Lọc theo trạng thái hoạt động: true (hiện), false (ẩn) (Tùy chọn).
     * @param minPrice  Mức giá thấp nhất để lọc (Tùy chọn).
     * @param maxPrice  Mức giá cao nhất để lọc (Tùy chọn).
     * @param sortBy    Tên trường dữ liệu dùng để sắp xếp (Mặc định: "productName").
     * @param sortDir   Chiều sắp xếp: "asc" (tăng dần) hoặc "desc" (giảm dần) (Mặc định: "asc").
     * @return Phản hồi HTTP 200 chứa Map dữ liệu gồm danh sách sản phẩm và thông tin phân trang.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
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