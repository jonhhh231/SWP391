package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.dto.logistics.AllocateRoutesRequest;
import com.groupSWP.centralkitchenplatform.dto.logistics.RouteAllocationResponse;
import com.groupSWP.centralkitchenplatform.service.inventory.RouteAllocationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics/orders")
@RequiredArgsConstructor
public class DemoOrderController {

    private final JdbcTemplate jdbcTemplate;

    // 🔥 Tiêm (Inject) cái Service xịn của bạn vào đây
    private final RouteAllocationService routeAllocationService;

    // 1. API Lấy Đơn hàng chờ bốc xếp
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/ready")
    public ResponseEntity<List<Map<String, Object>>> getReadyOrders() {
        String sql = "SELECT order_id, store_id as store_name, order_type, status FROM orders WHERE status = 'READY_TO_SHIP'";
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(orders);
    }

    // 2. API Lấy Lịch trình vận chuyển đang chạy
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status FROM shipments WHERE status IN ('PENDING', 'SHIPPING')";
        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(shipments);
    }

    // 3. API Xem chi tiết món hàng trên xe
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/{shipmentId}/details")
    public ResponseEntity<List<Map<String, Object>>> getShipmentDetails(@PathVariable String shipmentId) {
        String sql = "SELECT product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        List<Map<String, Object>> details = jdbcTemplate.queryForList(sql, shipmentId);
        return ResponseEntity.ok(details);
    }

    // 4. API Gán Tài xế
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @PostMapping("/{shipmentId}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable String shipmentId, @RequestBody Map<String, String> payload) {
        String driverName = payload.get("driverName");
        String vehiclePlate = payload.get("vehiclePlate");
        String sql = "UPDATE shipments SET driver_name = ?, vehicle_plate = ?, status = 'SHIPPING' WHERE shipment_id = ?";
        jdbcTemplate.update(sql, driverName, vehiclePlate, shipmentId);
        return ResponseEntity.ok(Map.of("message", "Gán tài xế thành công"));
    }

    // 5. 🔥 API CHẠY AI PHÂN BỔ TUYẾN (DÙNG LOGIC THẬT CỦA BẠN)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @PostMapping("/allocate-routes")
    public ResponseEntity<?> allocateRoutes(@RequestBody AllocateRoutesRequest request) {
        // Chuyền dữ liệu xuống file RouteAllocationService để nó chạy
        RouteAllocationResponse result = routeAllocationService.allocate(request);

        if (result.getTotalTripsCreated() == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Không có đơn hàng nào hợp lệ để tạo chuyến xe!"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Hệ thống AI đã phân bổ thành công " + result.getTotalTripsCreated() + " chuyến xe!"
        ));
    }

    // 6. 🔥 API LẤY LỊCH SỬ CHUYẾN XE (CHO TAB LỊCH SỬ BÊN REACT)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getCompletedShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status FROM shipments WHERE status = 'COMPLETED' ORDER BY updated_at DESC";
        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(shipments);
    }
}