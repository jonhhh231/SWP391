package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormulaRepository extends JpaRepository<Formula, Object> {


    // Lấy BOM theo list productId
    List<Formula> findByProductProductIdIn(List<String> productIds);
}