package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.inventory.StocktakeHistoryProjection;
import com.groupSWP.centralkitchenplatform.dto.inventory.StocktakeRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.InventoryLog;
import com.groupSWP.centralkitchenplatform.repositories.inventory.InventoryLogRepository;
import com.groupSWP.centralkitchenplatform.service.inventory.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;
    private final InventoryLogRepository inventoryLogRepository;

    /**
     * API Kiểm kê kho định kỳ (Stocktake).
     */
    @PostMapping("/stocktake")
    // 🌟 SỬA Ở ĐÂY: Dùng hasAnyAuthority bao trọn gói cả có ROLE_ và không có ROLE_
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> processStocktake(@Valid @RequestBody StocktakeRequest request) {
        stocktakeService.processStocktake(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Đã hoàn tất quá trình đối soát và kiểm kê kho!"));
    }

    // =========================================================
    // 🌟 API 1: LẤY DANH SÁCH LỊCH SỬ KIỂM KÊ (GOM NHÓM)
    // =========================================================
    @GetMapping("/stocktake/history")
    // 🌟 SỬA Ở ĐÂY: Mở cửa thêm cho Bếp trưởng (KITCHEN_MANAGER) vì họ cũng cần xem lịch sử kho
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<List<StocktakeHistoryProjection>> getStocktakeHistorySummary() {
        List<StocktakeHistoryProjection> history = inventoryLogRepository.getStocktakeHistorySummary();
        return ResponseEntity.ok(history);
    }

    // =========================================================
    // 🌟 API 2: LẤY CHI TIẾT 1 ĐỢT KIỂM KÊ KHI BẤM VÀO XEM
    // =========================================================
    @GetMapping("/stocktake/history/{sessionCode}")
    // 🌟 SỬA Ở ĐÂY: Tương tự như API 1
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<List<InventoryLog>> getStocktakeDetails(@PathVariable String sessionCode) {
        List<InventoryLog> details = inventoryLogRepository.findByReferenceCode(sessionCode);
        if (details.isEmpty()) {
            throw new RuntimeException("Không tìm thấy dữ liệu cho mã kiểm kê: " + sessionCode);
        }
        return ResponseEntity.ok(details);
    }
}