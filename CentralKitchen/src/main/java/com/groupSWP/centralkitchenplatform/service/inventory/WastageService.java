package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.InventoryLog;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import com.groupSWP.centralkitchenplatform.repositories.inventory.InventoryLogRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.FormulaRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.ProductionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WastageService {

    private final ProductionRunRepository productionRunRepository;
    private final FormulaRepository formulaRepository; // Đổi sang lấy Formula
    private final IngredientRepository ingredientRepository;
    private final InventoryLogRepository inventoryLogRepository; // Thêm repo ghi log

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

        // 3. Cập nhật waste_qty và ghi chú (reason) vào production_run
        run.setWasteQty(newWaste);
        run.setNote(request.getReason()); // LƯU LÝ DO HAO HỤT VÀO ĐÂY
        productionRunRepository.save(run);

        // 4. Trừ kho nguyên liệu CHUẨN THEO CÔNG THỨC (BOM)
        // SỬA LỖI 1: Gọi đúng tên hàm trong Repository
        List<Formula> formulas = formulaRepository.findByProduct_ProductId(run.getProduct().getProductId());

        for (Formula formula : formulas) {
            Ingredient ingredient = formula.getIngredient();

            BigDecimal actualIngredientWaste = request.getWasteQty().multiply(formula.getAmountNeeded());

            BigDecimal newStock = ingredient.getKitchenStock().subtract(actualIngredientWaste);
            if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                newStock = BigDecimal.ZERO;
            }
            ingredient.setKitchenStock(newStock);
            ingredientRepository.save(ingredient);

            // 5. LƯU VẾT VÀO INVENTORY_LOG
            InventoryLog log = new InventoryLog();

            // SỬA LỖI 2: Truyền Object thay vì truyền String ID
            log.setIngredient(ingredient);
            log.setProductionRun(run);

            log.setQuantityDeducted(actualIngredientWaste);
            log.setReferenceCode("WASTE-" + run.getRunId());
            log.setNote("Hao hụt sản xuất. Lý do: " + request.getReason());

            // FIX BẪY NULL: Bắt buộc phải set thời gian tạo
            log.setCreatedAt(java.time.LocalDateTime.now());

            // ⚠️ CẢNH BÁO ĐỎ: Entity InventoryLog ép buộc importItem không được null
            // Tạm thời set null nếu bạn chưa làm thuật toán FIFO,
            // NHƯNG để code chạy được, bạn phải vào InventoryLog đổi nullable = true
            // Hoặc lấy lô hàng cũ nhất ra gán vào đây: log.setImportItem(lô_cũ_nhất);

            inventoryLogRepository.save(log);
        }


        // 6. Trả về response
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