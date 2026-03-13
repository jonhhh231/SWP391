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

        // 1. Chỉ khởi tạo Kế hoạch mẻ nấu (PLANNED) - CHƯA TRỪ KHO VÀ CHƯA TÍNH TIỀN
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

        // 👉 ĐIỂM ĂN TIỀN: Đã gỡ bỏ vòng lặp trừ kho ở đây. Kế hoạch chỉ là kế hoạch, hàng trong kho vẫn nguyên si!

        return mapToResponse(savedRun);
    }

    // =========================================================================
    // 🌟 API MỚI: CHUYỂN TRẠNG THÁI MẺ NẤU (BẤM NÚT "NẤU" THÌ MỚI TRỪ KHO)
    // =========================================================================
    @Transactional
    public ProductionResponse changeProductionStatus(String runId, ProductionRun.ProductionStatus newStatus) {
        ProductionRun run = productionRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẻ nấu ID: " + runId));

        ProductionRun.ProductionStatus oldStatus = run.getStatus();

        if (oldStatus == newStatus) {
            return mapToResponse(run);
        }

        // 🌟 KỊCH BẢN: Đổi từ PLANNED (Kế hoạch) sang COOKING (Đang nấu) hoặc COMPLETED -> CHÍNH THỨC TRỪ KHO!
        if (oldStatus == ProductionRun.ProductionStatus.PLANNED &&
                (newStatus == ProductionRun.ProductionStatus.COOKING || newStatus == ProductionRun.ProductionStatus.COMPLETED)) {

            BigDecimal totalProductionCost = BigDecimal.ZERO;
            Product product = run.getProduct();

            // Lôi công thức ra và bắt đầu TRỪ KHO FIFO
            for (Formula formula : product.getFormulas()) {
                Ingredient ingredient = formula.getIngredient();

                // Tính tổng lượng nguyên liệu cần thiết cho toàn bộ mẻ nấu này
                BigDecimal totalNeeded = formula.getAmountNeeded().multiply(run.getPlannedQty());

                BigDecimal currentStock = ingredient.getKitchenStock() != null ? ingredient.getKitchenStock() : BigDecimal.ZERO;

                // Chốt chặn an toàn: Kiểm tra kho trước khi trừ
                if (currentStock.compareTo(totalNeeded) < 0) {
                    throw new RuntimeException("Không đủ nguyên liệu: " + ingredient.getName() +
                            ". Cần: " + totalNeeded + " " + ingredient.getUnit() +
                            ", Hiện có: " + currentStock);
                }

                // Chạy hàm FIFO trừ kho và tính tiền
                BigDecimal ingredientCost = deductIngredientWithFIFO(ingredient, totalNeeded, run);
                totalProductionCost = totalProductionCost.add(ingredientCost);
            }

            // Ghi nhận giá vốn thực tế vào mẻ nấu
            run.setTotalCostAtProduction(totalProductionCost);
        }

        // Chặn lùi trạng thái (Đã nấu rồi thì không được lùi về Kế hoạch để tránh lỗi kho kép)
        if (oldStatus == ProductionRun.ProductionStatus.COOKING && newStatus == ProductionRun.ProductionStatus.PLANNED) {
            throw new RuntimeException("Mẻ nấu đang diễn ra (COOKING), không thể lùi về trạng thái Kế Hoạch (PLANNED)!");
        }

        run.setStatus(newStatus);
        ProductionRun savedRun = productionRunRepository.save(run);

        return mapToResponse(savedRun);
    }

    public List<ProductionResponse> getActiveProductionRuns() {
        List<ProductionRun.ProductionStatus> activeStatuses = Arrays.asList(
                ProductionRun.ProductionStatus.PLANNED,
                ProductionRun.ProductionStatus.COOKING
        );
        List<ProductionRun> activeRuns = productionRunRepository.findByStatusInOrderByProductionDateDesc(activeStatuses);
        return activeRuns.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // --- HÀM HELPER CHUYỂN ĐỔI ENTITY SANG DTO ---
    private ProductionResponse mapToResponse(ProductionRun run) {
        return ProductionResponse.builder()
                .runId(run.getRunId())
                .productName(run.getProduct().getProductName())
                .plannedQty(run.getPlannedQty())
                .status(run.getStatus().name())
                .productionDate(run.getProductionDate())
                .build();
    }

    // =========================================================================
    // HÀM TRỪ KHO FIFO (PHIÊN BẢN HOÀN CHỈNH - GIỮ NGUYÊN 100% CODE CỦA SẾP)
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