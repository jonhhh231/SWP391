package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.UnitConversion;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
    // Tìm xem nguyên liệu này đã có quy đổi đơn vị này chưa?
    // VD: Dầu ăn đã có quy đổi "THÙNG" chưa?
    boolean existsByIngredientAndUnit(Ingredient ingredient, UnitType unit);
}