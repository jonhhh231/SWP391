package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // <--- Import cái này
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    // ^^^ THÊM: extends JpaSpecificationExecutor<Product>

    // 1. Hàm lấy danh sách category (Giữ nguyên)
    @Query("SELECT DISTINCT p.category.name FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    // 2. BỎ HÀM searchProducts @Query dài dòng cũ đi.
    // Chúng ta sẽ dùng hàm findAll(Specification, Pageable) có sẵn.
}