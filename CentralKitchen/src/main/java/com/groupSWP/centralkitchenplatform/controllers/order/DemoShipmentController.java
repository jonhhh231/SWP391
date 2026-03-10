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
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class DemoShipmentController {

    private final JdbcTemplate jdbcTemplate;
    private final ShipmentService shipmentService;

    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status FROM shipments WHERE status IN ('PENDING', 'SHIPPING', 'DELIVERED')";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PostMapping("/{shipmentId}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable String shipmentId, @RequestBody Map<String, String> payload) {
        String accountId = payload.get("accountId");
        String vehiclePlate = payload.get("vehiclePlate");

        if (accountId == null || accountId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn tài khoản COORDINATOR!"));
        }

        try {
            shipmentService.assignDriverToShipment(shipmentId, accountId, vehiclePlate);
            return ResponseEntity.ok(Map.of("message", "Gán tài xế thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{shipmentId}/delivered")
    public ResponseEntity<?> markAsDelivered(@PathVariable String shipmentId) {
        try {
            shipmentService.markShipmentAsDelivered(shipmentId);
            return ResponseEntity.ok(Map.of("message", "Đã xác nhận xe tới nơi! Chờ cửa hàng kiểm tra."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{shipmentId}/details")
    public ResponseEntity<List<Map<String, Object>>> getShipmentDetails(@PathVariable String shipmentId) {
        String sql = "SELECT product_id, product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, shipmentId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getOrderHistory() {
        String sql = "SELECT order_id, store_id as store_name, order_type, status FROM orders WHERE status IN ('DONE', 'CANCELLED') ORDER BY order_id DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // =========================================================================
    // 🔥 API MỚI BỔ SUNG: XỬ LÝ BÁO CÁO KIỂM HÀNG (DƯ/THIẾU) TỪ STORE MANAGER
    // =========================================================================
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'COORDINATOR', 'ROLE_COORDINATOR', 'MANAGER', 'ROLE_MANAGER', 'STORE_MANAGER', 'ROLE_STORE_MANAGER')")
    @PostMapping("/{shipmentId}/report")
    public ResponseEntity<?> reportReceivedShipment(
            @PathVariable String shipmentId,
            @RequestBody Map<String, Object> request) {

        try {
            // 1. Lấy nội dung ghi chú mà Cửa hàng gửi lên (generalNote)
            String finalNote = "Sự cố kiểm hàng";
            List<Map<String, Object>> reportedItems = (List<Map<String, Object>>) request.get("reportedItems");

            if (reportedItems != null && !reportedItems.isEmpty()) {
                String firstItemNote = (String) reportedItems.get(0).get("note");
                if (firstItemNote != null && !firstItemNote.trim().isEmpty()) {
                    finalNote = firstItemNote; // Ví dụ: "Thiếu 1 | Ghi chú chung: Bị đổ tháo"
                }
            }

            // 2. Cập nhật chuyến xe (shipments) thành ISSUE_REPORTED để nó biến mất khỏi tab "Lịch trình"
            String updateShipmentSql = "UPDATE shipments SET status = 'ISSUE_REPORTED' WHERE shipment_id = ?";
            jdbcTemplate.update(updateShipmentSql, shipmentId);

            // 3. Cập nhật Đơn hàng (orders) thành CANCELLED và nhét Ghi chú vào
            // Điều này làm cho đơn hàng hiện lên bảng Lịch sử (CANCELLED) và nhảy vào tab Xử lý khiếu nại (có chữ Note)
            String updateOrderSql = "UPDATE orders SET status = 'CANCELLED', note = ? WHERE shipment_id = ?";
            jdbcTemplate.update(updateOrderSql, finalNote, shipmentId);

            return ResponseEntity.ok(Map.of("message", "Đã chốt kiểm hàng! Chuyển qua bộ phận xử lý sự cố."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi xử lý báo cáo: " + e.getMessage()));
        }
    }
}