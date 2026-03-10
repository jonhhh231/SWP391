package com.groupSWP.centralkitchenplatform.controllers.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class DemoIncidentController {

    private final JdbcTemplate jdbcTemplate;

    // ✅ Không dùng bảng incidents. Lấy "khiếu nại" từ orders.note
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingIncidents() {
        String sql = """
            SELECT
              order_id AS id,
              store_id,
              note AS issue_description
            FROM orders
            WHERE status IN ('DONE', 'CANCELLED')
              AND note IS NOT NULL
              AND note <> ''
              AND note NOT LIKE '%Đã xử lý%'
            ORDER BY updated_at DESC
        """;
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }
}