package com.groupSWP.centralkitchenplatform.dto.logistics;

import lombok.Data;
import java.util.List;

@Data
public class ReportShipmentRequest {
    private List<OrderReport> reportedOrders;

    @Data
    public static class OrderReport {
        private String orderId;        // ID của đơn hàng (Order) bị thiếu
        private boolean isMissing;     // Xác nhận đơn này có bị thiếu/lỗi không
        private String issueNote;      // Ghi chú lỗi (Vd: "Thiếu 2 suất gà")
    }
}