package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;
import com.groupSWP.centralkitchenplatform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        // Lấy Role của User hiện tại (Giả sử Sếp lưu dạng "MANAGER", "ROLE_STORE")
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. XỬ LÝ LOGIC PHÂN QUYỀN (ĐÃ UPDATE THEO ENUM SYSTEM_ROLE) ---
        if (currentRole.equals("STORE_MANAGER")) {
            // Nếu là Quản lý cửa hàng: Ép cứng ID bằng chính ID của account đang login
            String loggedInStoreId = authentication.getName(); // Thay bằng hàm lấy ID của Sếp
            request.setStoreId(loggedInStoreId);
        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            // Nếu KHÔNG PHẢI Manager và KHÔNG PHẢI Store Manager (VD: Admin, Coordinator...) -> Đá văng!
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền tạo đơn!");
        }
        // Nếu lọt xuống đây tức là MANAGER -> Cho qua, dùng nguyên cái storeId truyền từ Postman

        // --- 3. GỌI SERVICE XỬ LÝ NHƯ CŨ ---
        OrderResponse response = orderService.createStandardOrder(request);
        return ResponseEntity.ok(response);
    }

    // API: POST /api/orders/urgent (ĐƠN KHẨN CẤP)
    @PostMapping("/urgent")
    public ResponseEntity<OrderResponse> createUrgentOrder(@RequestBody OrderRequest request) {

        // --- 1. LẤY THÔNG TIN NGƯỜI ĐANG ĐĂNG NHẬP ---
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. XỬ LÝ LOGIC PHÂN QUYỀN (Bảo mật y như đơn Tiêu chuẩn) ---
        if (currentRole.equals("STORE_MANAGER")) {
            String loggedInStoreId = authentication.getName(); // Sếp nhớ sửa chỗ này giống hàm cũ nhé
            request.setStoreId(loggedInStoreId);
        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền tạo đơn khẩn cấp!");
        }

        // --- 3. GỌI SERVICE XỬ LÝ ĐƠN KHẨN CẤP ---
        OrderResponse response = orderService.createUrgentOrder(request);
        return ResponseEntity.ok(response);
    }
}