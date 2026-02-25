package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.service.OrderService;
import com.groupSWP.centralkitchenplatform.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    // --- KHAI BÁO CÁC SERVICE TRÊN CÙNG CHO CHUẨN FORM ---
    private final ProductionService productionService;
    private final OrderService orderService; // Thêm OrderService để gọi hàm gom đơn

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

    @PostMapping("/cook")
    public ResponseEntity<ProductionResponse> cookProduct(@RequestBody ProductionRequest request) {
        return ResponseEntity.ok(productionService.createProductionRun(request));
    }

    // =====================================================================
    // API MỚI: TỔNG HỢP ĐƠN HÀNG (AGGREGATION)
    // =====================================================================
    @GetMapping("/aggregation")
    public ResponseEntity<List<KitchenAggregationResponse>> getPendingAggregation() {

        // --- 1. LẤY THÔNG TIN NGƯỜI ĐANG ĐĂNG NHẬP ---
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. KIỂM TRA QUYỀN (Bảo mật: Chỉ Bếp và Manager được xem) ---
        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền xem danh sách tổng hợp!");
        }

        // --- 3. GỌI SERVICE LẤY DANH SÁCH TỔNG HỢP ---
        List<KitchenAggregationResponse> response = orderService.getPendingProductionAggregation();

        return ResponseEntity.ok(response);
    }
}