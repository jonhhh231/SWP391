package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.dto.order.OrderDetailResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderHistoryResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;
import com.groupSWP.centralkitchenplatform.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class OrderController {

    private final OrderService orderService;

    // =======================================================
    // 1. MANAGER TẠO ĐƠN GIÚP CỬA HÀNG (Cần truyền storeId)
    // =======================================================
    @PostMapping("/standard")
    public ResponseEntity<OrderResponse> createStandardOrder(@RequestBody OrderRequest request) {
        if (request.getStoreId() == null || request.getStoreId().isEmpty()) {
            throw new IllegalArgumentException("Manager/Admin khi tạo đơn hộ phải chỉ định rõ mã cửa hàng (storeId)!");
        }
        log.info("Manager/Admin đang tạo đơn STANDARD cho cửa hàng {}", request.getStoreId());

        return ResponseEntity.ok(orderService.createOrder(request, false));
    }

    @PostMapping("/urgent")
    public ResponseEntity<OrderResponse> createUrgentOrder(@RequestBody OrderRequest request) {
        if (request.getStoreId() == null || request.getStoreId().isEmpty()) {
            throw new IllegalArgumentException("Manager/Admin khi tạo đơn hộ phải chỉ định rõ mã cửa hàng (storeId)!");
        }
        log.info("Manager/Admin đang tạo đơn URGENT cho cửa hàng {}", request.getStoreId());

        return ResponseEntity.ok(orderService.createOrder(request, true));
    }

    // =======================================================
    // 2. MANAGER XEM LỊCH SỬ ĐƠN (Của 1 tiệm cụ thể)
    // =======================================================
    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
            @RequestParam(required = true) String storeId) {
        // Lưu ý BA: @RequestParam(required = true) sẽ tự động ném lỗi HTTP 400 nếu Manager quên truyền storeId
        log.info("Manager/Admin đang xem lịch sử đơn của cửa hàng {}", storeId);

        return ResponseEntity.ok(orderService.getOrderHistory(storeId));
    }

    // =======================================================
    // 3. MANAGER XEM CHI TIẾT BẤT KỲ ĐƠN NÀO
    // =======================================================
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable String orderId) {
        log.info("Manager/Admin đang xem chi tiết đơn hàng {}", orderId);

        // Cứ thế mà lấy, không bị vướng rào cản "hàng xóm" như bên Cửa hàng
        return ResponseEntity.ok(orderService.getOrderDetail(orderId));
    }

    // =======================================================
    // 4. MANAGER HỦY BẤT KỲ ĐƠN NÀO (NẾU CÒN MỚI)
    // =======================================================
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {
        log.info("Manager/Admin đang thực hiện hủy đơn hàng {}", orderId);

        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Đã hủy đơn hàng " + orderId + " thành công!");
    }
}