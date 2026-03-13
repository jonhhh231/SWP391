package com.groupSWP.centralkitchenplatform.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý các truy vấn và logic liên quan đến điều phối vận chuyển.
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getReadyOrders() {
        // 🔥 Đã FIX LỖI: Dùng JOIN để lấy tên thật của cửa hàng thay vì hiển thị mã ID
        String sql = "SELECT o.order_id, s.name, o.order_type, o.status " +
                "FROM orders o " +
                "JOIN stores s ON o.store_id = s.store_id " +
                "WHERE o.status = 'READY_TO_SHIP' AND o.shipment_id IS NULL";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getActiveShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status " +
                "FROM shipments WHERE status IN ('PENDING', 'SHIPPING')";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getShipmentDetails(String shipmentId) {
        String sql = "SELECT product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        return jdbcTemplate.queryForList(sql, shipmentId);
    }

    public List<Map<String, Object>> getCompletedShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status " +
                "FROM shipments WHERE status IN ('DELIVERED', 'RESOLVED') ORDER BY updated_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getCoordinatorAccounts() {
        String sql = "SELECT account_id as id, username, role FROM accounts WHERE role = 'COORDINATOR'";
        List<Map<String, Object>> coordinators = jdbcTemplate.queryForList(sql);

        // ĐOẠN XỬ LÝ: Dịch mảng byte (Base64) sang chuỗi UUID chuẩn
        for (Map<String, Object> map : coordinators) {
            Object idObj = map.get("id");
            if (idObj instanceof byte[]) {
                byte[] bytes = (byte[]) idObj;
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                UUID uuid = new UUID(byteBuffer.getLong(), byteBuffer.getLong());
                map.put("id", uuid.toString());
            }
        }
        return coordinators;
    }
}