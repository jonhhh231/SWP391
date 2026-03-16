package com.groupSWP.centralkitchenplatform.controllers.store;

import com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse;
import com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.ProductionRun.ProductionStatus;
import com.groupSWP.centralkitchenplatform.service.order.OrderService;
import com.groupSWP.centralkitchenplatform.service.inventory.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final ProductionService productionService;
    private final OrderService orderService;

    /**
     * API Xem danh sách đơn hàng dành cho Bếp trung tâm.
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('KITCHEN_MANAGER')")
    public ResponseEntity<?> getKitchenOrders() {
        return ResponseEntity.ok("Danh sách đơn hàng của Bếp Trung Tâm");
    }

    /**
     * API Xóa công thức.
     */
    @DeleteMapping("/formula/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFormula(@PathVariable Long id) {
        return ResponseEntity.ok("Đã xóa công thức");
    }

    /**
     * API Lấy danh sách tổng hợp số lượng cần nấu (Aggregation).
     * <p>
     * Truy xuất và gom nhóm tất cả các món từ các đơn đặt hàng có trạng thái NEW.
     * Giúp Bếp trưởng có cái nhìn tổng quan về số lượng từng món cần chuẩn bị trong ngày.
     * </p>
     *
     * @return Phản hồi HTTP 200 chứa danh sách tổng hợp các món cần nấu.
     */
    @GetMapping("/aggregation")
    public ResponseEntity<List<KitchenAggregationResponse>> getPendingAggregation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền xem danh sách tổng hợp!");
        }

        List<KitchenAggregationResponse> response = orderService.getPendingProductionAggregation();
        return ResponseEntity.ok(response);
    }

    /**
     * API Chốt sổ gom đơn (Confirm Aggregation).
     * <p>
     * Khởi tạo các mẻ nấu (Production Run) với trạng thái PLANNED dựa trên kết quả gom đơn.
     * Lúc này tồn kho vật lý vẫn an toàn, chưa bị trừ liệu.
     * </p>
     *
     * @return Phản hồi HTTP 200 thông báo khởi tạo kế hoạch sản xuất thành công.
     */
    @PostMapping("/aggregation/confirm")
    public ResponseEntity<String> confirmProduction() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền chốt nấu!");
        }

        orderService.confirmProductionAndAggregateOrders();
        return ResponseEntity.ok("Đã chốt sổ gom đơn thành công! Hệ thống đã tạo danh sách các lệnh sản xuất (PLANNED) cho Bếp. Vui lòng bấm 'Bắt đầu nấu' để xuất kho!");
    }

    /**
     * API Xem danh sách các mẻ đang chờ nấu hoặc đang nấu (Active Productions).
     *
     * @return Phản hồi HTTP 200 chứa danh sách các mẻ nấu hiện hành.
     */
    @GetMapping("/productions/active")
    public ResponseEntity<List<ProductionResponse>> getActiveProductions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền xem danh sách mẻ nấu!");
        }

        return ResponseEntity.ok(productionService.getActiveProductionRuns());
    }

    /**
     * API Thay đổi trạng thái 1 mẻ nấu (Ví dụ: Chuyển sang COOKING để xuất kho).
     *
     * @param runId  Mã định danh của mẻ nấu.
     * @param status Trạng thái đích muốn chuyển sang.
     * @return Phản hồi HTTP 200 chứa thông tin mẻ nấu sau khi cập nhật.
     */
    @PutMapping("/productions/{runId}/status")
    public ResponseEntity<ProductionResponse> changeProductionStatus(
            @PathVariable String runId,
            @RequestParam ProductionStatus status) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền đổi trạng thái mẻ nấu!");
        }

        ProductionResponse response = productionService.changeProductionStatus(runId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * API Cập nhật trạng thái mẻ nấu hàng loạt (Bulk Update).
     * <p>Cho phép chọn nhiều mẻ cùng lúc để tiến hành nấu (giúp tự động xuất kho hàng loạt).</p>
     *
     * @param runIds Danh sách các mã định danh mẻ nấu (JSON Array).
     * @param status Trạng thái đích muốn chuyển sang.
     * @return Phản hồi HTTP 200 chứa danh sách các mẻ nấu đã được cập nhật.
     */
    @PutMapping("/productions/status/bulk")
    public ResponseEntity<List<ProductionResponse>> changeBulkProductionStatus(
            @RequestBody List<String> runIds,
            @RequestParam ProductionStatus status) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("KITCHEN_MANAGER") && !currentRole.equals("ROLE_KITCHEN_MANAGER")
                && !currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new AccessDeniedException("Chỉ Quản lý bếp hoặc Quản lý vận hành mới có quyền đổi trạng thái mẻ nấu hàng loạt!");
        }

        List<ProductionResponse> updatedRuns = new ArrayList<>();
        for (String runId : runIds) {
            ProductionResponse response = productionService.changeProductionStatus(runId, status);
            updatedRuns.add(response);
        }

        return ResponseEntity.ok(updatedRuns);
    }
}