package com.groupSWP.centralkitchenplatform.controllers.order; // Sếp giữ nguyên package của sếp

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class DemoShipmentController {

    private final JdbcTemplate jdbcTemplate;

    // (HÀM CŨ) API Lấy danh sách xe đang chạy/chờ
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status FROM shipments WHERE status IN ('PENDING', 'SHIPPING')";
        List<Map<String, Object>> shipments = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(shipments);
    }

    // 🔥 (HÀM MỚI CẦN THÊM VÀO) API Gán Tài xế và Biển số xe
    @PostMapping("/{shipmentId}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable String shipmentId, @RequestBody Map<String, String> payload) {
        String driverName = payload.get("driverName");
        String vehiclePlate = payload.get("vehiclePlate");

        // Cập nhật tên, biển số và đẩy trạng thái xe thành SHIPPING (Đang giao)
        String sql = "UPDATE shipments SET driver_name = ?, vehicle_plate = ?, status = 'SHIPPING' WHERE shipment_id = ?";
        jdbcTemplate.update(sql, driverName, vehiclePlate, shipmentId);

        return ResponseEntity.ok(Map.of("message", "Gán tài xế thành công"));
    }


    // 🔥 [OPTION 2]: Xem chi tiết món hàng trong chuyến xe
    @GetMapping("/{shipmentId}/details")
    public ResponseEntity<List<Map<String, Object>>> getShipmentDetails(@PathVariable String shipmentId) {
        // Móc vào bảng shipment_details mà AI của sếp đã tạo
        String sql = "SELECT product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        List<Map<String, Object>> details = jdbcTemplate.queryForList(sql, shipmentId);
        return ResponseEntity.ok(details);
    }

    // 🔥 [TÍNH NĂNG MỚI]: Lấy lịch sử các đơn hàng đã Hoàn thành hoặc Bị hủy
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getOrderHistory() {
        // Tìm những đơn hàng có status là DONE hoặc CANCELLED
        String sql = "SELECT order_id, store_id as store_name, order_type, status FROM orders WHERE status IN ('DONE', 'CANCELLED') ORDER BY order_id DESC";
        List<Map<String, Object>> historyOrders = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(historyOrders);
    }
}