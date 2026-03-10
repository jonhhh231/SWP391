package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.service.order.OrderDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/delivery")
@RequiredArgsConstructor
public class DeliveryFlowController {

    private final OrderDeliveryService deliveryService;

    // Kitchen Manager gọi
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ROLE_MANAGER', 'ADMIN')")
    @PostMapping("/{orderId}/preparing")
    public ResponseEntity<?> setPreparing(@PathVariable String orderId) {
        deliveryService.markAsPreparing(orderId);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái: Đang chuẩn bị"));
    }

    // Kitchen Manager gọi
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ROLE_MANAGER', 'ADMIN')")
    @PostMapping("/{orderId}/shipping")
    public ResponseEntity<?> setShipping(@PathVariable String orderId) {
        deliveryService.markAsShipping(orderId);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái: Đang giao hàng. Cửa hàng đã được thông báo!"));
    }

    // Store Manager gọi
    @PreAuthorize("hasAnyAuthority('STORE_MANAGER', 'ROLE_STORE_MANAGER', 'ADMIN')")
    @PostMapping("/{orderId}/confirm-receipt")
    public ResponseEntity<?> confirmReceipt(@PathVariable String orderId) {
        deliveryService.confirmReceipt(orderId);
        return ResponseEntity.ok(Map.of("message", "Xác nhận đã nhận hàng thành công!"));
    }
}