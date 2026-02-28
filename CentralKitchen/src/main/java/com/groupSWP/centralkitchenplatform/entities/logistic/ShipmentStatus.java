package com.groupSWP.centralkitchenplatform.entities.logistic;

public enum ShipmentStatus {
    PENDING,       // Chờ xử lý
    SHIPPING,      // Đang giao
    DELIVERED,     // Đã giao đủ
    ISSUE_REPORTED,// Đã giao nhưng có báo cáo thiếu/sai hàng
    RESOLVED       // Đã xử lý xong sự cố (đã lên đơn bù)
}
