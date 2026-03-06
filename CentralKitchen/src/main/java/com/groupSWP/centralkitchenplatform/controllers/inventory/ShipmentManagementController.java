package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistics/shipments")
@RequiredArgsConstructor
public class ShipmentManagementController {

    private final ShipmentManagementService shipmentManagementService;

    // Phân quyền: Chỉ ADMIN, MANAGER hoặc COORDINATOR của Bếp Trung Tâm mới được xuất bến
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('COORDINATOR')")
    @PatchMapping("/{id}/dispatch")
    public ResponseEntity<String> dispatchShipment(@PathVariable("id") String id) {
        shipmentManagementService.dispatchShipment(id);
        return ResponseEntity.ok("Chuyến xe " + id + " đã chính thức xuất bến thành công! Toàn bộ đơn hàng đã chuyển sang trạng thái Đang giao (SHIPPING).");
    }
}