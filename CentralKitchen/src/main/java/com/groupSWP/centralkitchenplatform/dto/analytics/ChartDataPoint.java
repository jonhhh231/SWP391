package com.groupSWP.centralkitchenplatform.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataPoint {
    private String date;        // Ngày (VD: "2026-02-10")
    private BigDecimal revenue; // Doanh thu
    private Long totalOrders;   // Số đơn
}