package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.order.OrderDetailResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderHistoryResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;
import com.groupSWP.centralkitchenplatform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders") // Đường dẫn gốc cho mọi API liên quan đến Order
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // API: POST /api/orders/standard
    @PostMapping("/standard")
    public ResponseEntity<OrderResponse> createStandardOrder(@RequestBody OrderRequest request) {

        // --- 1. LẤY THÔNG TIN NGƯỜI ĐANG ĐĂNG NHẬP (SECURITY) ---
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. XỬ LÝ LOGIC PHÂN QUYỀN ---
        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
            String loggedInStoreId = authentication.getName();
            request.setStoreId(loggedInStoreId);
        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền tạo đơn!");
        }

        // --- 3. GỌI SERVICE MỚI TẠO ĐƠN THƯỜNG (isUrgent = false) ---
        OrderResponse response = orderService.createOrder(request, false);
        return ResponseEntity.ok(response);
    }

    // API: POST /api/orders/urgent (ĐƠN KHẨN CẤP)
    @PostMapping("/urgent")
    public ResponseEntity<OrderResponse> createUrgentOrder(@RequestBody OrderRequest request) {

        // --- 1. LẤY THÔNG TIN NGƯỜI ĐANG ĐĂNG NHẬP ---
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. XỬ LÝ LOGIC PHÂN QUYỀN ---
        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
            String loggedInStoreId = authentication.getName();
            request.setStoreId(loggedInStoreId);
        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền tạo đơn khẩn cấp!");
        }

        // --- 3. GỌI SERVICE MỚI TẠO ĐƠN KHẨN CẤP (isUrgent = true) ---
        OrderResponse response = orderService.createOrder(request, true);
        return ResponseEntity.ok(response);
    }

    // API: GET /api/orders/history
    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
            @RequestParam(required = false) String storeId) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        String targetStoreId = storeId;

        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
            targetStoreId = authentication.getName();
        }
        else if (currentRole.equals("MANAGER") || currentRole.equals("ROLE_MANAGER")) {
            if (targetStoreId == null || targetStoreId.trim().isEmpty()) {
                throw new IllegalArgumentException("Quản lý vui lòng truyền storeId cần tra cứu!");
            }
        }
        else {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền xem lịch sử!");
        }

        List<OrderHistoryResponse> history = orderService.getOrderHistory(targetStoreId);
        return ResponseEntity.ok(history);
    }

    // API: GET /api/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable String orderId) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        OrderDetailResponse response = orderService.getOrderDetail(orderId);

        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
            String loggedInStoreId = authentication.getName();
            if (!response.getStoreId().equals(loggedInStoreId)) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem chi tiết đơn hàng của cửa hàng khác!");
            }
        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền xem chi tiết đơn hàng!");
        }

        return ResponseEntity.ok(response);
    }

    // API: PUT /api/orders/{orderId}/cancel
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        OrderDetailResponse orderDetail = orderService.getOrderDetail(orderId);

        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
            String loggedInStoreId = authentication.getName();
            if (!orderDetail.getStoreId().equals(loggedInStoreId)) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền hủy đơn hàng của cửa hàng khác!");
            }
        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền hủy đơn!");
        }

        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Đã hủy đơn hàng " + orderId + " thành công!");
    }
}