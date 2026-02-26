package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionRunRepository extends JpaRepository<ProductionRun, String> {

    Optional<ProductionRun> findByRunIdAndStatus(String runId, ProductionRun.ProductionStatus status);
}