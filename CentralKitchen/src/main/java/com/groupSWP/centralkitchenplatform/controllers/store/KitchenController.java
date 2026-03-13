package com.groupSWP.centralkitchenplatform.controllers.store;

import com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.service.order.OrderService;
import com.groupSWP.centralkitchenplatform.service.inventory.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final ProductionService productionService;
    private final OrderService orderService;

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

    // =====================================================================
    // 1. GOM ĐƠN (AGGREGATION) - XEM TRƯỚC DANH SÁCH
    // =====================================================================
    @GetMapping("/aggregation")
    public ResponseEntity<List<KitchenAggregationResponse>> getPendingAggregation() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền xem danh sách tổng hợp!");
        }

        List<KitchenAggregationResponse> response = orderService.getPendingProductionAggregation();
        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // 2. CHỐT SỔ GOM ĐƠN -> ĐẺ RA MẺ NẤU "PLANNED" (KHO VẪN AN TOÀN)
    // =====================================================================
    @PostMapping("/aggregation/confirm")
    public ResponseEntity<String> confirmProduction() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền chốt nấu!");
        }

        orderService.confirmProductionAndAggregateOrders();

        // 🌟 MỚI: Đã đổi câu thông báo cho chuẩn UX (Không lừa người dùng là đã trừ kho nữa)
        return ResponseEntity.ok("Đã chốt sổ gom đơn thành công! Hệ thống đã tạo danh sách các lệnh sản xuất (PLANNED) cho Bếp. Vui lòng bấm 'Bắt đầu nấu' để xuất kho!");
    }

    // =====================================================================
    // 3. XEM DANH SÁCH CÁC MẺ ĐANG CHỜ NẤU HOẶC ĐANG NẤU
    // =====================================================================
    @GetMapping("/productions/active")
    public ResponseEntity<List<ProductionResponse>> getActiveProductions() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền xem danh sách mẻ nấu!");
        }

        return ResponseEntity.ok(productionService.getActiveProductionRuns());
    }

    // =========================================================================
    // 4. API BẮT ĐẦU NẤU 1 MẺ (TRỪ KHO 1 MÓN)
    // =========================================================================
    @PutMapping("/productions/{runId}/status")
    public ResponseEntity<ProductionResponse> changeProductionStatus(
            @PathVariable String runId,
            @RequestParam com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun.ProductionStatus status) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền đổi trạng thái mẻ nấu!");
        }

        ProductionResponse response = productionService.changeProductionStatus(runId, status);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 🌟 5. VŨ KHÍ BÍ MẬT: NẤU HÀNG LOẠT (BULK UPDATE) -> BẤM 1 NÚT TRỪ KHO 50 MÓN
    // =========================================================================
    @PutMapping("/productions/status/bulk")
    public ResponseEntity<List<ProductionResponse>> changeBulkProductionStatus(
            @RequestBody List<String> runIds, // FE gửi lên mảng ["RUN-1", "RUN-2"]
            @RequestParam com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun.ProductionStatus status) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền đổi trạng thái mẻ nấu hàng loạt!");
        }

        // Chạy vòng lặp gọi lại hàm trừ kho cực xịn cho từng ID
        List<ProductionResponse> updatedRuns = new java.util.ArrayList<>();
        for (String runId : runIds) {
            ProductionResponse response = productionService.changeProductionStatus(runId, status);
            updatedRuns.add(response);
        }

        return ResponseEntity.ok(updatedRuns);
    }
}