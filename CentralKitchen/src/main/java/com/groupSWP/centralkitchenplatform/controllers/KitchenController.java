package com.groupSWP.centralkitchenplatform.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    @GetMapping("/orders")
    @PreAuthorize("hasRole('KITCHEN_MANAGER')") // Chỉ Quản lý bếp mới được xem
    public ResponseEntity<?> getKitchenOrders() {
        return ResponseEntity.ok("Danh sách đơn hàng của Bếp Trung Tâm");
    }

    @DeleteMapping("/formula/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được xóa công thức nấu ăn
    public ResponseEntity<?> deleteFormula(@PathVariable Long id) {
        return ResponseEntity.ok("Đã xóa công thức");
    }
}
