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
     //Bị lỗi run do trùng API
    // 1. API báo lỗi: Cho phép COORDINATOR hoặc MANAGER vào báo cáo giả lập
    // 🔥 ĐÃ SỬA: Bao phủ toàn bộ các role với hasAnyAuthority
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'COORDINATOR', 'ROLE_COORDINATOR', 'MANAGER', 'ROLE_MANAGER', 'STORE_MANAGER', 'ROLE_STORE_MANAGER')")
//    @PostMapping("/{shipmentId}/report")
//    public ResponseEntity<String> reportReceivedShipment(
//            @PathVariable String shipmentId,
//            @RequestBody ReportShipmentRequest request) {
//
//        String result = shipmentService.reportIssue(shipmentId, request);
//        return ResponseEntity.ok(result);
//    }

    // 2. API xử lý lỗi (Giao bù)
    // 🔥 ĐÃ SỬA: Bao phủ toàn bộ các role với hasAnyAuthority
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'COORDINATOR', 'ROLE_COORDINATOR', 'MANAGER', 'ROLE_MANAGER', 'STORE_MANAGER', 'ROLE_STORE_MANAGER')")
    @PostMapping("/{shipmentId}/resolve-replacement")
    public ResponseEntity<String> resolveAndCreateReplacement(
            @PathVariable String shipmentId) {

        String result = shipmentService.createReplacementShipment(shipmentId);
        return ResponseEntity.ok(result);
    }
}