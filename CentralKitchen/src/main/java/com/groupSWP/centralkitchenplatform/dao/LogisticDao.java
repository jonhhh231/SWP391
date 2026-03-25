package com.groupSWP.centralkitchenplatform.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository chuyên thực thi các câu lệnh SQL thuần (Native SQL) cho luồng Logistics.
 */
@Repository
@RequiredArgsConstructor
public class LogisticDao {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findReadyOrders() {
        String sql = "SELECT o.order_id, s.name, o.order_type, o.status " +
                "FROM orders o " +
                "JOIN stores s ON o.store_id = s.store_id " +
                "WHERE o.status = 'READY_TO_SHIP' AND o.shipment_id IS NULL";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findActiveShipments() {
        // 🔥 Đã thêm: store_name (Tên cửa hàng), address, order_id, shipment_type, created_at
        String sql = "SELECT sh.shipment_id, sh.driver_name AS driver, sh.vehicle_plate AS plate, sh.status, " +
                "sh.shipment_type, sh.created_at, " +
                "o.order_id, s.name AS store_name, s.address AS store_address " +
                "FROM shipments sh " +
                "LEFT JOIN orders o ON sh.shipment_id = o.shipment_id " +
                "LEFT JOIN stores s ON o.store_id = s.store_id " +
                "WHERE sh.status IN ('PENDING', 'SHIPPING') " +
                "ORDER BY sh.created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findShipmentDetails(String shipmentId) {
        String sql = "SELECT product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        return jdbcTemplate.queryForList(sql, shipmentId);
    }

    public List<Map<String, Object>> findCompletedShipments() {
        // 🔥 Đã thêm: store_name, order_id, shipment_type, delivered_at (thời gian giao thực tế)
        // Lưu ý: Đã bổ sung thêm trạng thái ISSUE_REPORTED (có báo cáo lỗi) vào lịch sử
        String sql = "SELECT sh.shipment_id, sh.driver_name AS driver, sh.vehicle_plate AS plate, sh.status, " +
                "sh.shipment_type, sh.delivered_at, sh.resolved_at, " +
                "o.order_id, s.name AS store_name " +
                "FROM shipments sh " +
                "LEFT JOIN orders o ON sh.shipment_id = o.shipment_id " +
                "LEFT JOIN stores s ON o.store_id = s.store_id " +
                "WHERE sh.status IN ('DELIVERED', 'ISSUE_REPORTED', 'RESOLVED') " +
                "ORDER BY sh.updated_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findCoordinatorAccountsRaw() {
        String sql = "SELECT a.account_id AS id, a.username, a.role, su.full_name AS fullName " +
                "FROM accounts a " +
                "LEFT JOIN system_users su ON a.account_id = su.account_id " +
                "WHERE a.role = 'COORDINATOR'";

        return jdbcTemplate.queryForList(sql);
    }
}