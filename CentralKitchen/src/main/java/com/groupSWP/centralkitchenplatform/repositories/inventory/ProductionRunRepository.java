package com.groupSWP.centralkitchenplatform.repositories.inventory;

import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.groupSWP.centralkitchenplatform.dto.analytics.ProductReportDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRunRepository extends JpaRepository<ProductionRun, String> {
    // Lùa tất cả các mẻ đang ở trạng thái chờ nấu (PLANNED) hoặc đang nấu (COOKING)
    List<ProductionRun> findByStatusInOrderByProductionDateDesc(List<ProductionRun.ProductionStatus> statuses);
    Optional<ProductionRun> findByRunIdAndStatus(String runId, ProductionRun.ProductionStatus status);

    // TÌM TOP MÓN HAO HỤT TRONG BẾP (Cháy khét, đổ vỡ...)
    @Query("SELECT new com.groupSWP.centralkitchenplatform.dto.analytics.ProductReportDto(" +
            "p.productId, p.productName, SUM(pr.wasteQty), SUM(pr.wasteQty * p.costPrice)) " +
            "FROM ProductionRun pr JOIN pr.product p " +
            "WHERE pr.productionDate >= :startDate AND pr.productionDate <= :endDate AND pr.wasteQty > 0 " +
            "GROUP BY p.productId, p.productName " +
            "ORDER BY SUM(pr.wasteQty) DESC")
    List<ProductReportDto> findTopWastedProductsInKitchen(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);
}