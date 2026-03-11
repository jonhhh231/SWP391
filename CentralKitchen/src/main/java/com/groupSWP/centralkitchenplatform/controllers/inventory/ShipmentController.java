package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // STORE MANAGER: Gọi API này để CHỐT HÀNG (Nhận đủ hoặc Báo thiếu)
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN')")
    @PostMapping("/{shipmentId}/report")
    public ResponseEntity<?> reportReceivedShipment(
            @PathVariable String shipmentId,
            @RequestBody(required = false) ReportShipmentRequest request) { // Không bắt buộc có body nếu nhận đủ

        try {
            String result = shipmentService.reportIssue(shipmentId, request);
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // KITCHEN MANAGER: Gọi API này để TẠO ĐƠN BÙ khi nhận được báo cáo thiếu hàng
    @PreAuthorize("hasAnyRole('KITCHEN_MANAGER', 'ADMIN')")
    @PostMapping("/{shipmentId}/resolve-replacement")
    public ResponseEntity<?> resolveAndCreateReplacement(@PathVariable String shipmentId) {
        try {
            String result = shipmentService.createReplacementShipment(shipmentId);
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}