package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.service.order.OrderDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý luồng trạng thái giao nhận và chuẩn bị đơn hàng (Delivery Flow).
 * <p>
 * Lớp này cung cấp các API để cập nhật tiến độ của một đơn hàng cụ thể
 * (từ lúc tiếp nhận, chuẩn bị trong bếp, cho đến khi sẵn sàng giao).
 * Thường được sử dụng bởi bộ phận Bếp trung tâm (Kitchen) để báo cáo tiến độ thực tế.
 * </p>
 */
@RestController
@RequestMapping("/api/orders/delivery")
@RequiredArgsConstructor
public class DeliveryFlowController {

    private final OrderDeliveryService deliveryService;

    // Kitchen Manager gọi
    /**
     * API Cập nhật trạng thái đơn hàng thành "Đang chuẩn bị" (PREPARING).
     * <p>
     * Hành động này đánh dấu việc Bếp trung tâm đã tiếp nhận đơn hàng và
     * bắt đầu quá trình nấu nướng, xuất kho nguyên liệu hoặc đóng gói.
     * </p>
     *
     * @param orderId Mã đơn hàng cần cập nhật trạng thái.
     * @return Phản hồi HTTP 200 kèm thông báo đã cập nhật thành công.
     */
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/{orderId}/preparing")
    public ResponseEntity<?> setPreparing(@PathVariable String orderId) {
        deliveryService.markAsPreparing(orderId);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái: Đang chuẩn bị"));
    }
}