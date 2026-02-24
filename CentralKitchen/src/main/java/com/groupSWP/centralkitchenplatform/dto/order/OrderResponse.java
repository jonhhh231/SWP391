package com.groupSWP.centralkitchenplatform.dto.order;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String status;
    private BigDecimal totalAmount;
    private String message;
    // Tạm thời trả về mấy thông tin cơ bản thế này cho lẹ Sếp nhé!
}