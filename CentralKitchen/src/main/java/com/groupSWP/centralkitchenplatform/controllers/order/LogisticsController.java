package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentService;
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
public class LogisticsController {

    private final JdbcTemplate jdbcTemplate;

    // 🔥 Tiêm ShipmentService vào để xử lý luồng tạo xe thủ công
    private final ShipmentService shipmentService;

    // 1. API Lấy Đơn hàng chờ bốc xếp (Để FE hiển thị cho Điều phối chọn)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @GetMapping("/ready")
    public ResponseEntity<List<Map<String, Object>>> getReadyOrders() {
        String sql = "SELECT order_id, store_id as store_name, order_type, status FROM orders WHERE status = 'READY_TO_SHIP' AND shipment_id IS NULL";
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(orders);
    }

    // 2. API Lấy Lịch trình vận chuyển đang chạy
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER',  'COORDINATOR')")
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status FROM shipments WHERE status IN ('PENDING', 'SHIPPING')";
        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(shipments);
    }

    // 3. API Xem chi tiết món hàng trên xe
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @GetMapping("/{shipmentId}/details")
    public ResponseEntity<List<Map<String, Object>>> getShipmentDetails(@PathVariable String shipmentId) {
        String sql = "SELECT product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        List<Map<String, Object>> details = jdbcTemplate.queryForList(sql, shipmentId);
        return ResponseEntity.ok(details);
    }

    // =========================================================================
    // 5. 🔥 API MỚI: TẠO CHUYẾN XE BẰNG TAY (THAY THẾ CHO AI)
    // =========================================================================
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @PostMapping("/manual-allocate")
    public ResponseEntity<?> allocateManualRoutes(@RequestBody Map<String, List<String>> payload) {
        List<String> orderIds = payload.get("orderIds");

        try {
            // Gọi hàm tạo xe thủ công mà mình đã viết cho bạn ở tin nhắn trước
            String message = shipmentService.createManualShipment(orderIds);
            return ResponseEntity.ok(Map.of("status", "success", "message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // 6. API LẤY LỊCH SỬ CHUYẾN XE
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getCompletedShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status FROM shipments WHERE status IN ('DELIVERED', 'RESOLVED') ORDER BY updated_at DESC";
        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(shipments);
    }

    // 7. API LẤY DANH SÁCH TÀI XẾ
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @GetMapping("/coordinators-list")
    public ResponseEntity<List<Map<String, Object>>> getCoordinatorAccounts() {
        String sql = "SELECT account_id as id, username, role FROM accounts WHERE role = 'COORDINATOR'";
        List<Map<String, Object>> coordinators = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(coordinators);
    }



}