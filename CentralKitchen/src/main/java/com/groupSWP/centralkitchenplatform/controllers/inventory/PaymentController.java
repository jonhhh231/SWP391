package com.groupSWP.centralkitchenplatform.controllers.inventory;


import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import com.groupSWP.centralkitchenplatform.service.inventory.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    // API 1: Lấy link thanh toán
    // API 1: Lấy link thanh toán
    @PostMapping("/create-url")
    public ResponseEntity<?> createPaymentUrl(@RequestParam String orderId, HttpServletRequest request) {
        try {
            String url = paymentService.createPaymentUrl(orderId, request);

            // 🔥 ĐÃ SỬA: Gói cả orderId và paymentUrl vào trong Map trả về
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "paymentUrl", url
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // API 2: Bắt kết quả trả về từ VNPay (KHÔNG DÙNG @PreAuthorize ở đây vì VNPay gọi)
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }

        String resultMessage = paymentService.processPaymentReturn(fields);
        // Trả về dòng chữ cho trình duyệt (Sau này có FE thì Redirect về trang của FE)
        return ResponseEntity.ok(resultMessage);
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String orderId) {

        // 1. Tìm đơn hàng dưới Database
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy mã đơn hàng này!"
                ));

        // 2. Gói các thông tin quan trọng nhất để trả về cho Frontend
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getOrderId());

        // Trạng thái tiền nong: UNPAID, PAID, FAILED...
        response.put("paymentStatus", order.getPaymentStatus());

        // Trạng thái nấu nướng: PENDING_PAYMENT, NEW, PROCESSING...
        response.put("orderStatus", order.getStatus());

        // Mã giao dịch của VNPay (nếu có)
        response.put("transactionNo", order.getTransactionId());

        // Thời gian khách quẹt thẻ thành công
        response.put("paymentDate", order.getPaymentDate());

        return ResponseEntity.ok(response);
    }
}