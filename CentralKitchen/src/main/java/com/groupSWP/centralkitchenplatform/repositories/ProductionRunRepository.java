package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionRunRepository extends JpaRepository<ProductionRun, String> {
    // Lùa tất cả các mẻ đang ở trạng thái chờ nấu (PLANNED) hoặc đang nấu (COOKING)
    List<ProductionRun> findByStatusInOrderByProductionDateDesc(List<ProductionRun.ProductionStatus> statuses);
}