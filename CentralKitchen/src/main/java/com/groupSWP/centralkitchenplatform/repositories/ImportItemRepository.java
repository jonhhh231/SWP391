package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ImportItemRepository extends JpaRepository<ImportItem, Long> {

    // TÌM CÁC LÔ CÒN HÀNG (quantity > 0), SẮP XẾP TỪ CŨ ĐẾN MỚI (Id Ascending)
    List<ImportItem> findByIngredientAndQuantityGreaterThanOrderByIdAsc(
            Ingredient ingredient,
            BigDecimal minQty
    );
}