package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // API báo lỗi: Tham số là String shipmentId
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')") // dòng này là để test cho dễ nào làm chỉnh lại sau
    @PostMapping("/{shipmentId}/report")
    public ResponseEntity<String> reportReceivedShipment(
            @PathVariable String shipmentId,
            @RequestBody ReportShipmentRequest request) {

        String result = shipmentService.reportIssue(shipmentId, request);
        return ResponseEntity.ok(result);
    }

    // API xử lý lỗi
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ADMIN') or hasAuthority('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')") // dòng này chỉ để test cho dễ nào làm thì sửa lại sao
    @PostMapping("/{shipmentId}/resolve-replacement")
    public ResponseEntity<String> resolveAndCreateReplacement(
            @PathVariable String shipmentId) {

        String result = shipmentService.createReplacementShipment(shipmentId);
        return ResponseEntity.ok(result);
    }
}