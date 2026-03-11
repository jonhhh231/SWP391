package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @PostMapping("/{shipmentId}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable String shipmentId, @RequestBody Map<String, String> payload) {
        String accountId = payload.get("accountId");

        if (accountId == null || accountId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng truyền accountId của tài xế!"));
        }

        try {
            shipmentService.assignDriverToShipment(shipmentId, accountId);
            return ResponseEntity.ok(Map.of("message", "Gán tài xế thành công! Bắt đầu tính giờ giao hàng."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // 2. API BỊ THIẾU: TÀI XẾ BÁO ĐÃ TỚI NƠI
    // =========================================================================
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @PostMapping("/{shipmentId}/delivered")
    public ResponseEntity<?> markAsDelivered(@PathVariable String shipmentId) {
        try {
            shipmentService.markShipmentAsDelivered(shipmentId);
            return ResponseEntity.ok(Map.of("message", "Đã xác nhận xe tới nơi! Chờ Cửa hàng trưởng kiểm tra."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // 3. STORE MANAGER: CHỐT HÀNG (Đã đổi sang hasAnyAuthority)
    // =========================================================================
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    @PostMapping("/{shipmentId}/report")
    public ResponseEntity<?> reportReceivedShipment(
            @PathVariable String shipmentId,
            @RequestBody(required = false) ReportShipmentRequest request) {

        try {
            String result = shipmentService.reportIssue(shipmentId, request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 4. KITCHEN MANAGER: TẠO ĐƠN BÙ (Đã đổi sang hasAnyAuthority)
    // =========================================================================
    @PreAuthorize("hasAnyRole('KITCHEN_MANAGER', 'ADMIN')")
    @PostMapping("/{shipmentId}/resolve-replacement")
    public ResponseEntity<?> resolveAndCreateReplacement(@PathVariable String shipmentId) {
        try {
            String result = shipmentService.createReplacementShipment(shipmentId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}