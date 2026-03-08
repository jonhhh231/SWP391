package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.InventoryLog;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.inventory.ImportItemRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.InventoryLogRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.ProductionRunRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.ProductRepository;
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

        // 1. Khởi tạo mẻ nấu (Chưa có giá vốn)
        ProductionRun run = new ProductionRun();
        run.setRunId(generatedRunId);
        run.setProduct(product);
        run.setPlannedQty(request.getQuantity());
        run.setActualQty(BigDecimal.ZERO);
        run.setWasteQty(BigDecimal.ZERO);
        run.setTotalCostAtProduction(BigDecimal.ZERO);
        run.setProductionDate(LocalDateTime.now());
        run.setStatus(ProductionRun.ProductionStatus.PLANNED);
        run.setBatchCode("BATCH-" + System.currentTimeMillis());

        ProductionRun savedRun = productionRunRepository.save(run);

        BigDecimal totalProductionCost = BigDecimal.ZERO;

        // 2. Duyệt qua từng nguyên liệu trong công thức để trừ kho
        for (Formula formula : product.getFormulas()) {
            Ingredient ingredient = formula.getIngredient();

            // Theo thiết kế: amountNeeded đã là ĐƠN VỊ GỐC (Base Unit)
            BigDecimal amountPerUnit = formula.getAmountNeeded();

            // Tính tổng lượng nguyên liệu cần thiết cho toàn bộ mẻ nấu này
            BigDecimal totalNeeded = amountPerUnit.multiply(request.getQuantity());

            BigDecimal currentStock = ingredient.getKitchenStock() != null ? ingredient.getKitchenStock() : BigDecimal.ZERO;

            // Chốt chặn an toàn: Kiểm tra kho trước khi trừ
            if (currentStock.compareTo(totalNeeded) < 0) {
                throw new RuntimeException("Không đủ nguyên liệu: " + ingredient.getName() +
                        ". Cần: " + totalNeeded + " " + ingredient.getUnit() +
                        ", Hiện có: " + currentStock);
            }

            // Gọi hàm trừ kho FIFO và lấy về tổng giá vốn của lượng nguyên liệu bị trừ
            BigDecimal ingredientCost = deductIngredientWithFIFO(ingredient, totalNeeded, savedRun);
            totalProductionCost = totalProductionCost.add(ingredientCost);
        }

        // 3. Cập nhật lại giá vốn tổng cho mẻ nấu
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
    // HÀM TRỪ KHO FIFO (PHIÊN BẢN HOÀN CHỈNH - CÓ GHI LOG OBJECT VÀ TÍNH GIÁ VỐN)
    // =========================================================================
    private BigDecimal deductIngredientWithFIFO(Ingredient ingredient, BigDecimal quantityNeeded, ProductionRun run) {
        BigDecimal remainingToDeduct = quantityNeeded;
        BigDecimal totalIngredientCost = BigDecimal.ZERO;

        // Lấy danh sách các lô hàng còn tồn của nguyên liệu này, xếp theo lô cũ nhất (ID tăng dần)
        List<ImportItem> availableBatches = importItemRepository
                .findByIngredientAndRemainingQuantityGreaterThanOrderByIdAsc(ingredient, BigDecimal.ZERO);

        for (ImportItem batch : availableBatches) {
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal batchQty = batch.getRemainingQuantity();
            BigDecimal deductedAmount;

            if (batchQty.compareTo(remainingToDeduct) >= 0) {
                // Lô này đủ sức gánh toàn bộ số lượng cần trừ
                batch.setRemainingQuantity(batchQty.subtract(remainingToDeduct));
                deductedAmount = remainingToDeduct;
                remainingToDeduct = BigDecimal.ZERO;
            } else {
                // Lô này không đủ, trừ hết lô này rồi chuyển sang lô tiếp theo
                batch.setRemainingQuantity(BigDecimal.ZERO);
                deductedAmount = batchQty;
                remainingToDeduct = remainingToDeduct.subtract(batchQty);
            }
            importItemRepository.save(batch);

            // Tính tiền lô hàng này (Số lượng bị trừ * Giá nhập của lô)
            BigDecimal costForThisBatch = deductedAmount.multiply(batch.getImportPrice());
            totalIngredientCost = totalIngredientCost.add(costForThisBatch);

            // Ghi nhận biến động kho vào Log
            InventoryLog log = InventoryLog.builder()
                    .importItem(batch)
                    .ingredient(ingredient)
                    .productionRun(run)
                    .quantityDeducted(deductedAmount)
                    .note("Trừ kho tự động FIFO cho mẻ nấu: " + run.getRunId())
                    .createdAt(LocalDateTime.now())
                    .build();
            inventoryLogRepository.save(log);
        }

        // Chốt chặn cuối: Nếu duyệt hết kho mà vẫn thiếu (Lỗi đồng bộ dữ liệu)
        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("LỖI NGHIÊM TRỌNG: Tồn kho các lô không khớp với tổng tồn. Thiếu: " + remainingToDeduct);
        }

        // Trừ tổng tồn kho chung của nguyên liệu
        ingredient.setKitchenStock(ingredient.getKitchenStock().subtract(quantityNeeded));
        ingredientRepository.save(ingredient);

        return totalIngredientCost;
    }
}