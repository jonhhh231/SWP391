package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT p FROM Product p WHERE " +
            // 1. Tìm theo tên (chứa từ khóa, không phân biệt hoa thường)
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            // 2. Lọc chính xác theo danh mục (Category)
            "AND (:category IS NULL OR :category = '' OR p.category = :category) " +
            // 3. Lọc theo trạng thái (Active/Inactive) -> Quan trọng cho Franchise Store
            "AND (:isActive IS NULL OR p.isActive = :isActive) " +
            // 4. Lọc theo giá bán thấp nhất (>= minPrice)
            "AND (:minPrice IS NULL OR p.sellingPrice >= :minPrice) " +
            // 5. Lọc theo giá bán cao nhất (<= maxPrice)
            "AND (:maxPrice IS NULL OR p.sellingPrice <= :maxPrice)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("isActive") Boolean isActive,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable // Chứa thông tin: trang số mấy (page), lấy bao nhiêu dòng (size), sắp xếp (sort)
    );
}