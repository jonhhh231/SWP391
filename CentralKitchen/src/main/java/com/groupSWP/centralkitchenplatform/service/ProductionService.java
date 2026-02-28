package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.InventoryLog;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionRunRepository productionRunRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final ImportItemRepository importItemRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional
    public ProductionResponse createProductionRun(ProductionRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại!"));

        if (product.getFormulas() == null || product.getFormulas().isEmpty()) {
            throw new RuntimeException("Món này chưa có công thức (BOM), không thể nấu!");
        }

        String generatedRunId = "RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 🔥 UPDATE V2.2: LƯU MẺ NẤU TRƯỚC ĐỂ LẤY OBJECT CHO KHÓA NGOẠI (FK) CỦA LOG
        ProductionRun run = new ProductionRun();
        run.setRunId(generatedRunId);
        run.setProduct(product);
        run.setPlannedQty(request.getQuantity());
        run.setActualQty(BigDecimal.ZERO);
        run.setWasteQty(BigDecimal.ZERO);
        run.setTotalCostAtProduction(BigDecimal.ZERO); // Khởi tạo giá vốn = 0
        run.setProductionDate(LocalDateTime.now());
        run.setStatus(ProductionRun.ProductionStatus.PLANNED);
        run.setBatchCode("BATCH-" + System.currentTimeMillis());

        ProductionRun savedRun = productionRunRepository.save(run);

        // Biến cộng dồn tổng chi phí (Giá vốn) của toàn bộ mẻ nấu này
        BigDecimal totalProductionCost = BigDecimal.ZERO;

        for (Formula formula : product.getFormulas()) {
            Ingredient ingredient = formula.getIngredient();
            BigDecimal amountPerUnit = formula.getAmountNeeded();
            BigDecimal totalNeeded = amountPerUnit.multiply(request.getQuantity());
            BigDecimal currentStock = ingredient.getKitchenStock() != null ? ingredient.getKitchenStock() : BigDecimal.ZERO;

            if (currentStock.compareTo(totalNeeded) < 0) {
                throw new RuntimeException("Không đủ nguyên liệu: " + ingredient.getName() +
                        ". Cần: " + totalNeeded + " " + ingredient.getUnit() +
                        ", Hiện có: " + currentStock);
            }

            // 🔥 UPDATE V2.2: TRUYỀN HẲN OBJECT `savedRun` VÀO ĐỂ GHI LOG.
            // Hàm này giờ xịn hơn: Trừ xong nó trả về CHI PHÍ của lượng nguyên liệu vừa trừ!
            BigDecimal ingredientCost = deductIngredientWithFIFO(ingredient, totalNeeded, savedRun);

            // Cộng dồn vào giá vốn tổng
            totalProductionCost = totalProductionCost.add(ingredientCost);
        }

        // 🔥 UPDATE V2.2: Cập nhật lại giá vốn thực tế cuối cùng cho mẻ nấu
        savedRun.setTotalCostAtProduction(totalProductionCost);
        productionRunRepository.save(savedRun);

        return ProductionResponse.builder()
                .runId(savedRun.getRunId())
                .productName(product.getProductName())
                .plannedQty(savedRun.getPlannedQty())
                .status(savedRun.getStatus().name())
                .productionDate(savedRun.getProductionDate())
                .build();
    }

    public List<ProductionResponse> getActiveProductionRuns() {
        // ... (Giữ nguyên code cũ của Sếp) ...
        List<ProductionRun.ProductionStatus> activeStatuses = Arrays.asList(
                ProductionRun.ProductionStatus.PLANNED,
                ProductionRun.ProductionStatus.COOKING
        );
        List<ProductionRun> activeRuns = productionRunRepository.findByStatusInOrderByProductionDateDesc(activeStatuses);
        return activeRuns.stream().map(run -> ProductionResponse.builder()
                .runId(run.getRunId())
                .productName(run.getProduct().getProductName())
                .plannedQty(run.getPlannedQty())
                .status(run.getStatus().name())
                .productionDate(run.getProductionDate())
                .build()
        ).collect(Collectors.toList());
    }

    // =========================================================================
    // HÀM TRỪ KHO FIFO (PHIÊN BẢN 2.2 - CÓ GHI LOG OBJECT VÀ TÍNH GIÁ VỐN)
    // =========================================================================
    private BigDecimal deductIngredientWithFIFO(Ingredient ingredient, BigDecimal quantityNeeded, ProductionRun run) {

        BigDecimal remainingToDeduct = quantityNeeded;
        BigDecimal totalIngredientCost = BigDecimal.ZERO; // Máy tính tiền

        // 🔥 UPDATE: Tìm các lô hàng có remainingQuantity > 0
        List<ImportItem> availableBatches = importItemRepository
                .findByIngredientAndRemainingQuantityGreaterThanOrderByIdAsc(ingredient, BigDecimal.ZERO);

        for (ImportItem batch : availableBatches) {
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

            // 🔥 UPDATE: Thao tác trên cột RemainingQuantity
            BigDecimal batchQty = batch.getRemainingQuantity();
            BigDecimal deductedAmount;

            if (batchQty.compareTo(remainingToDeduct) >= 0) {
                batch.setRemainingQuantity(batchQty.subtract(remainingToDeduct));
                deductedAmount = remainingToDeduct;
                remainingToDeduct = BigDecimal.ZERO;
            } else {
                batch.setRemainingQuantity(BigDecimal.ZERO);
                deductedAmount = batchQty;
                remainingToDeduct = remainingToDeduct.subtract(batchQty);
            }
            importItemRepository.save(batch);

            // 🔥 UPDATE KẾ TOÁN: Tính tiền lô hàng này (SL bị trừ * Giá nhập của lô)
            BigDecimal costForThisBatch = deductedAmount.multiply(batch.getImportPrice());
            totalIngredientCost = totalIngredientCost.add(costForThisBatch);

            // ==========================================
            // GHI BIÊN BẢN (LOG) LẠI NGAY LẬP TỨC!
            // ==========================================
            InventoryLog log = InventoryLog.builder()
                    .importItem(batch)           // Truyền nguyên Object
                    .ingredient(ingredient)      // Truyền nguyên Object
                    .productionRun(run)          // Truyền nguyên Object
                    .quantityDeducted(deductedAmount)
                    .note("Hệ thống tự động trừ kho FIFO cho mẻ nấu: " + run.getRunId())
                    .createdAt(LocalDateTime.now())
                    .build();
            inventoryLogRepository.save(log);
        }

        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("LỖI NGHIÊM TRỌNG: Tồn kho các lô không đủ. Thiếu: " + remainingToDeduct);
        }

        ingredient.setKitchenStock(ingredient.getKitchenStock().subtract(quantityNeeded));
        ingredientRepository.save(ingredient);

        // Trả về số tiền tổng cộng vừa trừ để hàm cha lưu vào ProductionRun
        return totalIngredientCost;
    }
}