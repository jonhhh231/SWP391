package com.groupSWP.centralkitchenplatform.repositories.product;

import com.groupSWP.centralkitchenplatform.entities.product.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // <--- Import cái này
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    // ^^^ THÊM: extends JpaSpecificationExecutor<Product>

    // 1. Hàm lấy danh sách category (Giữ nguyên)
    @Query("SELECT DISTINCT p.category.name FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    // 👉 THÊM HÀM MỚI NÀY: Lấy Product KÈM THEO toàn bộ Formulas và Ingredient của nó
    @EntityGraph(attributePaths = {"formulas", "formulas.ingredient"})
    Optional<Product> findById(String id);
    List<Product> findByIsActiveTrue();
}