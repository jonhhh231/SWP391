package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.service.KitchenAggregationService;
import com.groupSWP.centralkitchenplatform.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final ProductionService productionService;
    private final KitchenAggregationService kitchenAggregationService;

    @GetMapping("/orders")
    @PreAuthorize("hasRole('KITCHEN_MANAGER')")
    public ResponseEntity<?> getKitchenOrders() {
        return ResponseEntity.ok("Danh sách đơn hàng của Bếp Trung Tâm");
    }

    @DeleteMapping("/formula/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFormula(@PathVariable Long id) {
        return ResponseEntity.ok("Đã xóa công thức");
    }

    @PostMapping("/cook")
    public ResponseEntity<ProductionResponse> cookProduct(@RequestBody ProductionRequest request) {
        return ResponseEntity.ok(productionService.createProductionRun(request));
    }

    @GetMapping("/aggregation")
    @PreAuthorize("hasAnyRole('KITCHEN_STAFF','KITCHEN_MANAGER')")
    public KitchenAggregationResponse aggregation(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Order.DeliveryWindow deliveryWindow,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(defaultValue = "false") boolean includeIngredients
    ) {
        LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        return kitchenAggregationService.aggregate(d, deliveryWindow, status, includeIngredients);
    }
}