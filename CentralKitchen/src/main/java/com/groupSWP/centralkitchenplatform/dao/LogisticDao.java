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
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status " +
                "FROM shipments WHERE status IN ('PENDING', 'SHIPPING')";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findShipmentDetails(String shipmentId) {
        String sql = "SELECT product_name, expected_quantity FROM shipment_details WHERE shipment_id = ?";
        return jdbcTemplate.queryForList(sql, shipmentId);
    }

    public List<Map<String, Object>> findCompletedShipments() {
        String sql = "SELECT shipment_id, driver_name as driver, vehicle_plate as plate, status " +
                "FROM shipments WHERE status IN ('DELIVERED', 'RESOLVED') ORDER BY updated_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findCoordinatorAccountsRaw() {
        String sql = "SELECT account_id as id, username, role FROM accounts WHERE role = 'COORDINATOR'";
        return jdbcTemplate.queryForList(sql);
    }
}