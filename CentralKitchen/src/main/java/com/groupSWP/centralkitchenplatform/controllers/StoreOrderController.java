package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.order.OrderDetailResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderHistoryResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
import com.groupSWP.centralkitchenplatform.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final OrderService orderService;
    private final AccountRepository accountRepository; // Bơm cái này vào để check quyền

    // =======================================================
    // HÀM HELPER: MÓC STORE_ID TỪ TOKEN ĐĂNG NHẬP
    // =======================================================
    private String getStoreIdFromPrincipal(Principal principal) {
        String username = principal.getName();
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (account.getStore() == null) {
            throw new RuntimeException("Tài khoản chưa được liên kết với Cửa hàng nào!");
        }
        return account.getStore().getStoreId();
    }

    // =======================================================
    // 1. API LẤY DANH SÁCH ĐƠN HÀNG CỦA CỬA HÀNG
    // =======================================================
    @GetMapping
    public ResponseEntity<?> getOrderHistory(Principal principal) {
        try {
            // Tự động moi storeId chuẩn xác từ Token
            String storeId = getStoreIdFromPrincipal(principal);
            log.info("Cửa hàng {} đang xem lịch sử đơn hàng", storeId);

            // Gọi hàm CÓ SẴN của Sếp
            List<OrderHistoryResponse> history = orderService.getOrderHistory(storeId);
            return ResponseEntity.ok(history);

        } catch (RuntimeException e) {
            log.error("Lỗi khi xem lịch sử đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =======================================================
    // 2. API XEM CHI TIẾT 1 ĐƠN HÀNG (KÈM CHỐNG XEM TRỘM)
    // =======================================================
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(Principal principal, @PathVariable String orderId) {
        try {
            String storeId = getStoreIdFromPrincipal(principal);
            log.info("Cửa hàng {} đang xem chi tiết đơn {}", storeId, orderId);

            // Gọi hàm CÓ SẴN của Sếp
            OrderDetailResponse detail = orderService.getOrderDetail(orderId);

            // 🛡️ BẢO MẬT: Chặn đứng nếu Cửa hàng cố tình nhập ID đơn của tiệm khác
            if (!detail.getStoreId().equals(storeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Lỗi bảo mật: Bạn không có quyền xem đơn hàng của cửa hàng khác!");
            }

            return ResponseEntity.ok(detail);

        } catch (RuntimeException e) {
            log.error("Lỗi khi xem chi tiết đơn {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =======================================================
    // 3. API TẠO ĐƠN HÀNG TIÊU CHUẨN (STANDARD)
    // =======================================================
    @PostMapping("/standard")
    public ResponseEntity<?> createStandardOrder(Principal principal, @RequestBody OrderRequest request) {
        try {
            String realStoreId = getStoreIdFromPrincipal(principal);
            request.setStoreId(realStoreId);

            log.info("Cửa hàng {} bóp cò đơn STANDARD", realStoreId);

            // Gọi hàm gộp All-in-one
            OrderResponse response = orderService.createOrder(request, false);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("🔥 Lỗi tạo đơn Standard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // =======================================================
    // 4. API TẠO ĐƠN HÀNG KHẨN CẤP (URGENT)
    // =======================================================
    @PostMapping("/urgent")
    public ResponseEntity<?> createUrgentOrder(Principal principal, @RequestBody OrderRequest request) {
        try {
            String realStoreId = getStoreIdFromPrincipal(principal);
            request.setStoreId(realStoreId);

            log.info("Cửa hàng {} bóp cò đơn URGENT", realStoreId);

            // Gọi hàm gộp All-in-one với flag isUrgent = true
            OrderResponse response = orderService.createOrder(request, true);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("🔥 Lỗi tạo đơn Urgent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}