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
        log.info("Manager bắt đầu xử lý Kiểm kê kho định kỳ...");
        int totalDiscrepancies = 0;
        boolean hasSevereLoss = false; // Cờ theo dõi xem có vụ mất cắp/hao hụt nghiêm trọng nào không

        for (StocktakeRequest.StocktakeItem item : request.getItems()) {
            Ingredient ingredient = ingredientRepository.findById(item.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu: " + item.getIngredientId()));

            BigDecimal systemQty = ingredient.getKitchenStock() != null ? ingredient.getKitchenStock() : BigDecimal.ZERO;
            BigDecimal actualQty = item.getActualQty();

            // Tính độ chênh lệch: Hệ thống - Thực tế đếm bằng tay
            BigDecimal discrepancy = systemQty.subtract(actualQty);

            // =========================================================
            // 🔥 CHỐT CHẶN: VƯỢT RÀO HAO HỤT NGHIÊM TRỌNG (FORCE CONFIRM)
            // =========================================================
            if (systemQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal variancePercentage = discrepancy.abs()
                        .divide(systemQty, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                // Nếu lệch > 50% MÀ KHÔNG CÓ CỜ FORCE CONFIRM -> Chặn lại báo cho Manager
                if (variancePercentage.compareTo(new BigDecimal("50")) > 0 && !item.isForceConfirm()) {
                    throw new RuntimeException("🛑 CẢNH BÁO: Số lượng kiểm kê của [" + ingredient.getName() + "] chênh lệch tới " + variancePercentage.intValue() + "% so với hệ thống! Nếu đây là hao hụt THỰC TẾ, Quản lý vui lòng tick chọn 'Xác nhận hao hụt bất thường' để tiếp tục ghi đè!");
                }

                // Nếu được phép vượt rào -> Bật cờ báo động đỏ
                if (variancePercentage.compareTo(new BigDecimal("50")) > 0 && item.isForceConfirm()) {
                    hasSevereLoss = true;
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
            // NẾU KIỂM KÊ KHỚP HOẶC DƯ
            else if (discrepancy.compareTo(BigDecimal.ZERO) < 0) {
                ingredient.setKitchenStock(actualQty);
                ingredientRepository.save(ingredient);

                // Ghi log đơn giản là điều chỉnh tăng kho
                InventoryLog logEntry = InventoryLog.builder()
                        .ingredient(ingredient)
                        .quantityDeducted(discrepancy) // Số âm thể hiện việc cộng thêm vào
                        .note("Kiểm kê bởi Manager: Điều chỉnh tăng kho. Ghi chú: " + item.getNote())
                        .createdAt(LocalDateTime.now())
                        .build();
                inventoryLogRepository.save(logEntry);
            }
        }

        // =========================================================
        // 🔥 GỬI THÔNG BÁO TỚI BẾP TRƯỞNG & ADMIN (MANAGER LÀ NGƯỜI LÀM NÊN KHÔNG NHẬN)
        // =========================================================
        if (hasSevereLoss) {
            // CÓ HAO HỤT LỚN (>50%) -> Báo động đỏ URGENT
            notificationService.broadcastNotification(
                    List.of("ADMIN", "KITCHEN_MANAGER"),
                    "🚨 BÁO ĐỘNG: HAO HỤT KHO NGHIÊM TRỌNG",
                    "Phát hiện nguyên liệu bị thất thoát trên 50% sau kiểm kê. Quản lý (Manager) đã phải dùng quyền Ghi đè hệ thống. Bếp trưởng vui lòng rà soát lại nhân viên ngay lập tức!",
                    Notification.NotificationType.URGENT
            );
        } else if (totalDiscrepancies > 0) {
            // HAO HỤT NHỎ BÌNH THƯỜNG -> Báo vàng WARNING
            notificationService.broadcastNotification(
                    List.of("ADMIN", "KITCHEN_MANAGER"),
                    "⚠️ BÁO CÁO KIỂM KÊ KHO",
                    "Quản lý (Manager) vừa hoàn tất kiểm kê định kỳ. Phát hiện " + totalDiscrepancies + " nguyên liệu có sự chênh lệch (Hao hụt nhẹ) so với sổ sách.",
                    Notification.NotificationType.WARNING
            );
        }
    }

    // =========================================================================
    // HÀM TRỪ KHO FIFO DÀNH RIÊNG CHO KIỂM KÊ
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

            String finalNote = "Hao hụt kiểm kê kho bởi Manager. " + (userNote != null ? "- Ghi chú: " + userNote : "");

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