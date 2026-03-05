package com.groupSWP.centralkitchenplatform.dto.logistics;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteAllocationRequest {
    private String coordinatorId;       // Mã người điều phối (VD: NV001)
    private List<String> orderIds;      // Danh sách ID các đơn hàng cần gom
    private String driverName;          // Tên tài xế
    private String vehiclePlate;        // Biển số xe
    private LocalDateTime deliveryDate; // Ngày giờ dự kiến giao
    private String shipmentType;        // STANDARD hoặc REPLACEMENT
}