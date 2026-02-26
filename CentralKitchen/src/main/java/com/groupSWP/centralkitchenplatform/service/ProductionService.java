package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.ImportItemRepository;
import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductionRunRepository;
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
            deductIngredientWithFIFO(ingredient, totalNeeded);
        }

        // 4. Tạo Lệnh Sản Xuất (ProductionRun)
        ProductionRun run = new ProductionRun();
        run.setRunId("RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
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
    // HÀM TRỪ KHO NÂNG CAO THEO CHIẾN LƯỢC FIFO (NHẬP TRƯỚC - XUẤT TRƯỚC)
    // =========================================================================
    private void deductIngredientWithFIFO(Ingredient ingredient, BigDecimal quantityNeeded) {

        BigDecimal remainingToDeduct = quantityNeeded; // Cục nợ cần trừ

        // 1. Lùa các lô hàng của nguyên liệu này (ID cũ lên trước, lô còn hàng > 0)
        List<ImportItem> availableBatches = importItemRepository
                .findByIngredientAndQuantityGreaterThanOrderByIdAsc(ingredient, BigDecimal.ZERO);

        // 2. Vòng lặp vắt kiệt từng lô
        for (ImportItem batch : availableBatches) {

            // Nếu đã hết nợ thì bẻ cua thoát vòng lặp ngay
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal batchQty = batch.getQuantity();

            if (batchQty.compareTo(remainingToDeduct) >= 0) {
                // KỊCH BẢN A: Lô to, đủ gánh hết nợ
                batch.setQuantity(batchQty.subtract(remainingToDeduct));
                importItemRepository.save(batch);
                remainingToDeduct = BigDecimal.ZERO;
            } else {
                // KỊCH BẢN B: Lô nhỏ, vắt cạn rồi qua lô sau tìm tiếp
                batch.setQuantity(BigDecimal.ZERO);
                importItemRepository.save(batch);
                remainingToDeduct = remainingToDeduct.subtract(batchQty);
            }
        }

        // 3. Quét sạch kho vẫn thiếu -> Quăng lỗi
        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("LỖI NGHIÊM TRỌNG: Tồn kho các lô không đủ để trừ. Còn thiếu: "
                    + remainingToDeduct + " cho nguyên liệu: " + ingredient.getName());
        }

        // 4. Cập nhật tổng tồn kho (Kích hoạt bảo vệ Optimistic Locking)
        ingredient.setKitchenStock(ingredient.getKitchenStock().subtract(quantityNeeded));
        ingredientRepository.save(ingredient);
    }
}