package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.FormulaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Nhớ import cái này nhé

@Repository
public interface FormulaRepository extends JpaRepository<Formula, FormulaKey> {

    // Hàm dùng để xóa công thức cũ (bạn đã có)
    void deleteByProduct_ProductId(String productId);

    // 👇 BẠN BẮT BUỘC PHẢI THÊM HÀM NÀY VÀO
    // Để Service có thể lấy được danh sách công thức theo ID sản phẩm
    List<Formula> findByProduct_ProductId(String productId);
}