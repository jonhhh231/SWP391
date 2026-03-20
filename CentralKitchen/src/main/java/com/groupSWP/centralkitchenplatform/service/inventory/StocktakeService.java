package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.inventory.StocktakeRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.kitchen.InventoryLog;
import com.groupSWP.centralkitchenplatform.entities.notification.Notification;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.repositories.inventory.ImportItemRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.InventoryLogRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final IngredientRepository ingredientRepository;
    private final ImportItemRepository importItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final NotificationService notificationService;

    @Transactional
    public void processStocktake(StocktakeRequest request) {
        log.info("Bắt đầu xử lý Kiểm kê kho định kỳ...");
        int totalDiscrepancies = 0;

        for (StocktakeRequest.StocktakeItem item : request.getItems()) {
            Ingredient ingredient = ingredientRepository.findById(item.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu: " + item.getIngredientId()));

            BigDecimal systemQty = ingredient.getKitchenStock() != null ? ingredient.getKitchenStock() : BigDecimal.ZERO;
            BigDecimal actualQty = item.getActualQty();

            // Tính độ chênh lệch: Hệ thống - Thực tế đếm bằng tay
            BigDecimal discrepancy = systemQty.subtract(actualQty);

            // =========================================================
            // 🔥 CHỐT CHẶN CHỐNG NHẬP NHẦM (HUMAN ERROR GUARDRAIL)
            // =========================================================
            if (systemQty.compareTo(BigDecimal.ZERO) > 0) {
                // Tính % lệch: (Độ lệch tuyệt đối / Tồn kho hệ thống) * 100
                BigDecimal variancePercentage = discrepancy.abs()
                        .divide(systemQty, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                // Nếu nhập lệch quá 50% tồn kho -> Nghi ngờ gõ nhầm số! Chặn luôn!
                if (variancePercentage.compareTo(new BigDecimal("50")) > 0) {
                    throw new RuntimeException("🛑 CẢNH BÁO LỖI NHẬP LIỆU: Số lượng kiểm kê của [" + ingredient.getName() + "] chênh lệch tới " + variancePercentage.intValue() + "% so với hệ thống! Vui lòng đếm lại hoặc kiểm tra xem có gõ thiếu số không!");
                }
            }
            // =========================================================

            // NẾU CÓ HAO HỤT (Hệ thống > Thực tế) -> Chạy FIFO để trừ bớt các lô hàng cũ
            if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("Phát hiện hao hụt nguyên liệu {}: Hệ thống = {}, Thực tế = {}", ingredient.getName(), systemQty, actualQty);

                // Thuật toán FIFO y chang lúc nấu ăn để tiền vốn khớp 100%
                deductIngredientWithFIFO(ingredient, discrepancy, item.getNote());

                // Cập nhật lại tồn kho tổng
                ingredient.setKitchenStock(actualQty);
                ingredientRepository.save(ingredient);

                totalDiscrepancies++;
            }
            // NẾU KIỂM KÊ KHỚP HOẶC DƯ (Thường hiếm khi dư, nếu dư thì chỉ cập nhật lại số liệu)
            else if (discrepancy.compareTo(BigDecimal.ZERO) < 0) {
                ingredient.setKitchenStock(actualQty);
                ingredientRepository.save(ingredient);

                // Ghi log đơn giản là điều chỉnh tăng kho
                InventoryLog logEntry = InventoryLog.builder()
                        .ingredient(ingredient)
                        .quantityDeducted(discrepancy) // Số âm thể hiện việc cộng thêm vào
                        .note("Kiểm kê: Điều chỉnh tăng kho. Ghi chú: " + item.getNote())
                        .createdAt(LocalDateTime.now())
                        .build();
                inventoryLogRepository.save(logEntry);
            }
        }

        // 🔥 THÔNG BÁO: Bắn Notification cho ADMIN (để soi tài chính)
        // và KITCHEN_MANAGER (để biết kho đã được cập nhật thực tế)
        if (totalDiscrepancies > 0) {
            notificationService.broadcastNotification(
                    List.of("ADMIN", "KITCHEN_MANAGER"),
                    "⚠️ BÁO CÁO KIỂM KÊ KHO",
                    "Vừa hoàn tất kiểm kê định kỳ. Phát hiện " + totalDiscrepancies + " nguyên liệu có sự chênh lệch (Hao hụt). Vui lòng kiểm tra báo cáo chi tiết!",
                    Notification.NotificationType.WARNING
            );
        }
    }

    // =========================================================================
    // HÀM TRỪ KHO FIFO DÀNH RIÊNG CHO KIỂM KÊ (Tái sử dụng logic siêu xịn của Sếp)
    // =========================================================================
    private void deductIngredientWithFIFO(Ingredient ingredient, BigDecimal quantityNeeded, String userNote) {
        BigDecimal remainingToDeduct = quantityNeeded;

        List<ImportItem> availableBatches = importItemRepository
                .findByIngredientAndRemainingQuantityGreaterThanOrderByIdAsc(ingredient, BigDecimal.ZERO);

        for (ImportItem batch : availableBatches) {
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

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

            // Ghi Log lịch sử để sau này Admin truy vết được tại sao lô hàng này bị trừ
            String finalNote = "Hao hụt kiểm kê kho định kỳ. " + (userNote != null ? "- Ghi chú: " + userNote : "");

            InventoryLog log = InventoryLog.builder()
                    .importItem(batch)
                    .ingredient(ingredient)
                    .quantityDeducted(deductedAmount)
                    .note(finalNote)
                    .createdAt(LocalDateTime.now())
                    .referenceCode("STOCKTAKE-" + System.currentTimeMillis())
                    .build();
            inventoryLogRepository.save(log);
        }

        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            log.error("CẢNH BÁO: Hao hụt nhiều hơn cả số lượng trong các lô hàng. Đã trừ sạch các lô!");
        }
    }
}