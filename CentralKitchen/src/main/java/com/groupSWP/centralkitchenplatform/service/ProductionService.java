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
        // 1. Tìm món ăn cần nấu
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại!"));

        // 2. Kiểm tra xem món này có công thức chưa
        if (product.getFormulas() == null || product.getFormulas().isEmpty()) {
            throw new RuntimeException("Món này chưa có công thức (BOM), không thể nấu!");
        }

        // 3. DUYỆT QUA CÔNG THỨC ĐỂ TRỪ KHO NGUYÊN LIỆU
        // Logic: Duyệt từng nguyên liệu -> Tính lượng cần -> Kiểm tra tồn -> Trừ
        String generatedRunId = "RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        for (Formula formula : product.getFormulas()) {
            Ingredient ingredient = formula.getIngredient();

            // Tính lượng cần: (Định lượng 1 phần) * (Số lượng muốn nấu)
            // Lưu ý: formula.getAmountNeeded() trong code cũ bạn để là Double,
            // nên convert sang BigDecimal để tính toán chuẩn xác.
            BigDecimal amountPerUnit = formula.getAmountNeeded();
            BigDecimal totalNeeded = amountPerUnit.multiply(request.getQuantity());

            // Lấy tồn kho hiện tại (Nếu null thì coi là 0)
            BigDecimal currentStock = ingredient.getKitchenStock() != null ? ingredient.getKitchenStock() : BigDecimal.ZERO;

            // Kiểm tra tồn kho
            if (currentStock.compareTo(totalNeeded) < 0) {
                throw new RuntimeException("Không đủ nguyên liệu: " + ingredient.getName() +
                        ". Cần: " + totalNeeded + " " + ingredient.getUnit() +
                        ", Hiện có: " + currentStock);
            }

            // [VŨ KHÍ MỚI] GỌI CỖ MÁY FIFO RA XỬ LÝ TRỪ TỪNG LÔ HÀNG!
            // Hàm này sẽ tự động trừ lô cũ trước, lô mới sau, và cập nhật luôn kitchenStock
            deductIngredientWithFIFO(ingredient, totalNeeded, generatedRunId);
        }

        // 4. Tạo Lệnh Sản Xuất (ProductionRun)
        ProductionRun run = new ProductionRun();
        run.setRunId(generatedRunId);
        run.setProduct(product);
        run.setPlannedQty(request.getQuantity());
        run.setActualQty(BigDecimal.ZERO); // Mới nhận đơn đẩy xuống, chưa ra lò được con gà nào nên thực tế = 0
        run.setWasteQty(BigDecimal.ZERO);
        run.setProductionDate(LocalDateTime.now());
        run.setStatus(ProductionRun.ProductionStatus.PLANNED); // Chuyển trạng thái thành "Đã lên kế hoạch" (Đang chờ bếp xào nấu)

        // BatchCode có thể gen theo ngày: BATCH-20240202
        run.setBatchCode("BATCH-" + System.currentTimeMillis());

        ProductionRun savedRun = productionRunRepository.save(run);

        // 5. Trả về kết quả (DTO)
        return ProductionResponse.builder()
                .runId(savedRun.getRunId())
                .productName(product.getProductName())
                .plannedQty(savedRun.getPlannedQty())
                .status(savedRun.getStatus().name())
                .productionDate(savedRun.getProductionDate())
                .build();
    }

    // =========================================================================
    // HÀM LẤY DANH SÁCH CÁC MẺ ĐANG TRÊN LÒ (PLANNED, COOKING)
    // =========================================================================
    public List<ProductionResponse> getActiveProductionRuns() {

        // 1. Chỉ định lấy những mẻ chưa xong (Chờ nấu hoặc Đang nấu)
        List<ProductionRun.ProductionStatus> activeStatuses = Arrays.asList(
                ProductionRun.ProductionStatus.PLANNED,
                ProductionRun.ProductionStatus.COOKING
        );

        // 2. Gọi DB lùa ra danh sách
        List<ProductionRun> activeRuns = productionRunRepository.findByStatusInOrderByProductionDateDesc(activeStatuses);

        // 3. Đóng gói trả về DTO cho mượt
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
    // HÀM TRỪ KHO FIFO (PHIÊN BẢN 2.0 - CÓ GHI LOG LƯU VẾT ERP)
    // =========================================================================
    private void deductIngredientWithFIFO(Ingredient ingredient, BigDecimal quantityNeeded, String referenceCode) {

        BigDecimal remainingToDeduct = quantityNeeded;
        List<ImportItem> availableBatches = importItemRepository
                .findByIngredientAndQuantityGreaterThanOrderByIdAsc(ingredient, BigDecimal.ZERO);

        for (ImportItem batch : availableBatches) {
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal batchQty = batch.getQuantity();
            BigDecimal deductedAmount;

            if (batchQty.compareTo(remainingToDeduct) >= 0) {
                batch.setQuantity(batchQty.subtract(remainingToDeduct));
                deductedAmount = remainingToDeduct;
                remainingToDeduct = BigDecimal.ZERO;
            } else {
                batch.setQuantity(BigDecimal.ZERO);
                deductedAmount = batchQty;
                remainingToDeduct = remainingToDeduct.subtract(batchQty);
            }
            importItemRepository.save(batch);

            // ==========================================
            // GHI BIÊN BẢN (LOG) LẠI NGAY LẬP TỨC!
            // ==========================================
            InventoryLog log = InventoryLog.builder()
                    .importItemId(batch.getId())
                    .ingredientId(ingredient.getIngredientId())
                    .quantityDeducted(deductedAmount)
                    .referenceCode(referenceCode)
                    .note("Hệ thống tự động trừ kho FIFO cho mẻ nấu: " + referenceCode)
                    .createdAt(LocalDateTime.now())
                    .build();
            inventoryLogRepository.save(log);
        }

        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("LỖI NGHIÊM TRỌNG: Tồn kho các lô không đủ. Thiếu: " + remainingToDeduct);
        }

        ingredient.setKitchenStock(ingredient.getKitchenStock().subtract(quantityNeeded));
        ingredientRepository.save(ingredient);
    }
}