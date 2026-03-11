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
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/{orderId}/preparing")
    public ResponseEntity<?> setPreparing(@PathVariable String orderId) {
        deliveryService.markAsPreparing(orderId);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái: Đang chuẩn bị"));
    }
}