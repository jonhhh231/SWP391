package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.order.OrderDetailResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderHistoryResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
import com.groupSWP.centralkitchenplatform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AccountRepository accountRepository; // Bơm vào để "móc" dữ liệu thực

    // =======================================================
    // HÀM HELPER: CHUYỂN USERNAME TOKEN -> STORE_ID THẬT
    // =======================================================
    private String getActualStoreId(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống!"));

        if (account.getStore() == null) {
            throw new RuntimeException("Tài khoản này chưa được gán cho bất kỳ Cửa hàng nào!");
        }
        return account.getStore().getStoreId(); // Đây mới là ID chuẩn để Hibernate query
    }

    @PostMapping("/standard")
    public ResponseEntity<OrderResponse> createStandardOrder(@RequestBody OrderRequest request) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (currentRole.contains("STORE_MANAGER")) {
            // FIX TẠI ĐÂY: Lấy ID thật từ DB
            String realStoreId = getActualStoreId(authentication.getName());
            request.setStoreId(realStoreId);
        }
        else if (!currentRole.contains("MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Quyền hạn không đủ!");
        }

        OrderResponse response = orderService.createOrder(request, false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/urgent")
    public ResponseEntity<OrderResponse> createUrgentOrder(@RequestBody OrderRequest request) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if (currentRole.contains("STORE_MANAGER")) {
            // FIX TẠI ĐÂY: Lấy ID thật từ DB
            String realStoreId = getActualStoreId(authentication.getName());
            request.setStoreId(realStoreId);
        }
        else if (!currentRole.contains("MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Quyền hạn không đủ!");
        }

        OrderResponse response = orderService.createOrder(request, true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
            @RequestParam(required = false) String storeId) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        String targetStoreId = storeId;

        if (currentRole.contains("STORE_MANAGER")) {
            // FIX TẠI ĐÂY
            targetStoreId = getActualStoreId(authentication.getName());
        }
        else if (currentRole.contains("MANAGER")) {
            if (targetStoreId == null || targetStoreId.trim().isEmpty()) {
                throw new IllegalArgumentException("Quản lý vui lòng truyền storeId!");
            }
        }

        List<OrderHistoryResponse> history = orderService.getOrderHistory(targetStoreId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable String orderId) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        OrderDetailResponse response = orderService.getOrderDetail(orderId);

        if (currentRole.contains("STORE_MANAGER")) {
            // FIX TẠI ĐÂY: So sánh Store ID thật
            String loggedInStoreId = getActualStoreId(authentication.getName());
            if (!response.getStoreId().equals(loggedInStoreId)) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn không thể xem đơn của hàng xóm!");
            }
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        OrderDetailResponse orderDetail = orderService.getOrderDetail(orderId);

        if (currentRole.contains("STORE_MANAGER")) {
            // FIX TẠI ĐÂY
            String loggedInStoreId = getActualStoreId(authentication.getName());
            if (!orderDetail.getStoreId().equals(loggedInStoreId)) {
                throw new org.springframework.security.access.AccessDeniedException("Không được hủy đơn người khác!");
            }
        }

        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Đã hủy đơn hàng " + orderId + " thành công!");
    }
}