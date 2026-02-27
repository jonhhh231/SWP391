package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WastageService {

    private final ProductionRunRepository productionRunRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public WastageResponse recordWastage(WastageRequest request) {

        // 1. Tìm production run đang COOKING
        ProductionRun run = productionRunRepository
                .findByRunIdAndStatus(request.getRunId(), ProductionRun.ProductionStatus.COOKING)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy lượt sản xuất đang ở trạng thái ĐANG NẤU với id: " + request.getRunId()));

        // 2. Validate waste không vượt planned
        BigDecimal currentWaste = run.getWasteQty() != null ? run.getWasteQty() : BigDecimal.ZERO;
        BigDecimal newWaste = currentWaste.add(request.getWasteQty());

        if (newWaste.compareTo(run.getPlannedQty()) > 0) {
            throw new IllegalArgumentException(
                    "Số lượng hao hụt vượt quá số lượng dự kiến: " + run.getPlannedQty());
        }

        // 3. Cập nhật waste_qty vào production_run
        run.setWasteQty(newWaste);
        productionRunRepository.save(run);

        // 4. Trừ kitchen_stock của ingredients liên quan
        List<Ingredient> ingredients = ingredientRepository
                .findByFormulas_Product_ProductId(run.getProduct().getProductId());

        for (Ingredient ingredient : ingredients) {
            BigDecimal newStock = ingredient.getKitchenStock().subtract(request.getWasteQty());
            if (newStock.compareTo(BigDecimal.ZERO) < 0) newStock = BigDecimal.ZERO;
            ingredient.setKitchenStock(newStock);
        }
        ingredientRepository.saveAll(ingredients);

        // 5. Trả về response
        return WastageResponse.builder()
                .runId(run.getRunId())
                .productId(run.getProduct().getProductId())
                .productName(run.getProduct().getProductName())
                .plannedQty(run.getPlannedQty())
                .actualQty(run.getActualQty())
                .wasteQty(newWaste)
                .status(run.getStatus().name())
                .message("Ghi nhận hao hụt thành công")
                .build();
    }
}