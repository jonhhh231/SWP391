package com.groupSWP.centralkitchenplatform.repositories.inventory;

import com.groupSWP.centralkitchenplatform.entities.kitchen.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // Đếm số lần lệch kho
    @Query("SELECT COUNT(l) FROM InventoryLog l WHERE l.createdAt BETWEEN :startDate AND :endDate AND l.note LIKE '%Hao hụt kiểm kê%'")
    long countStocktakeDiscrepancies(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Lấy chi tiết lịch sử lệch kho
    @Query("SELECT l FROM InventoryLog l JOIN FETCH l.ingredient WHERE l.createdAt BETWEEN :startDate AND :endDate AND l.note LIKE '%Hao hụt kiểm kê%' ORDER BY l.createdAt DESC")
    List<InventoryLog> findStocktakeLogs(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}