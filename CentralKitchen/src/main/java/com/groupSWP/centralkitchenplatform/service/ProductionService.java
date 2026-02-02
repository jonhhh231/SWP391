package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionRunRepository productionRunRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

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

            // Trừ kho và cập nhật
            ingredient.setKitchenStock(currentStock.subtract(totalNeeded));
            ingredientRepository.save(ingredient);
        }

        // 4. Tạo Lệnh Sản Xuất (ProductionRun)
        ProductionRun run = new ProductionRun();
        run.setRunId("RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        run.setProduct(product);
        run.setPlannedQty(request.getQuantity());
        run.setActualQty(request.getQuantity()); // Tạm thời giả định nấu thành công 100%
        run.setWasteQty(BigDecimal.ZERO);
        run.setProductionDate(LocalDateTime.now());
        run.setStatus(ProductionRun.ProductionStatus.COMPLETED); // Đã trừ kho xong -> Hoàn thành

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
}