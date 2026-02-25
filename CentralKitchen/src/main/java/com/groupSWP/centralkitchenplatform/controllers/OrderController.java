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

        // Lấy Role của User hiện tại (Giả sử Sếp lưu dạng "MANAGER", "ROLE_STORE")
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. XỬ LÝ LOGIC PHÂN QUYỀN (ĐÃ UPDATE THEO ENUM SYSTEM_ROLE) ---
        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
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
        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
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

    // API: GET /api/orders/history
    // Quản lý truyền thêm param: /api/orders/history?storeId=ST001
    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
            @RequestParam(required = false) String storeId) {

        // --- 1. LẤY THÔNG TIN NGƯỜI ĐANG ĐĂNG NHẬP ---
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
        String targetStoreId = storeId;

        // --- 2. XỬ LÝ LOGIC PHÂN QUYỀN ---
        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {
            // Nếu là Cửa hàng: Ép cứng ID bằng ID đang login (mặc kệ nó truyền param gì lên URL)
            targetStoreId = authentication.getName(); // Sếp nhớ sửa lại chỗ này giống mấy hàm trước nhé
        }
        else if (currentRole.equals("MANAGER") || currentRole.equals("ROLE_MANAGER")) {
            // Nếu là Manager: Bắt buộc phải truyền storeId lên URL để biết muốn xem sổ của ai
            if (targetStoreId == null || targetStoreId.trim().isEmpty()) {
                throw new IllegalArgumentException("Quản lý vui lòng truyền storeId cần tra cứu!");
            }
        }
        else {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền xem lịch sử!");
        }

        // --- 3. GỌI SERVICE TRẢ VỀ DANH SÁCH ---
        List<OrderHistoryResponse> history = orderService.getOrderHistory(targetStoreId);
        return ResponseEntity.ok(history);
    }

    // API: GET /api/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable String orderId) {

        // --- 1. LẤY THÔNG TIN NGƯỜI ĐANG ĐĂNG NHẬP ---
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        // --- 2. GỌI SERVICE MOI RUỘT ĐƠN HÀNG TRƯỚC ---
        // (Phải lấy ra để biết cái đơn này thuộc về storeId nào)
        OrderDetailResponse response = orderService.getOrderDetail(orderId);

        // --- 3. XỬ LÝ LOGIC PHÂN QUYỀN (CHỐNG NHÌN TRỘM) ---
        if (currentRole.equals("STORE_MANAGER") || currentRole.equals("ROLE_STORE_MANAGER")) {

            String loggedInStoreId = authentication.getName();

            // Nếu ID Cửa hàng đang login KHÁC với ID Cửa hàng sở hữu đơn hàng -> Đá văng!
            if (!response.getStoreId().equals(loggedInStoreId)) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem chi tiết đơn hàng của cửa hàng khác!");
            }

        }
        else if (!currentRole.equals("MANAGER") && !currentRole.equals("ROLE_MANAGER")) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý vận hành hoặc Quản lý cửa hàng mới có quyền xem chi tiết đơn hàng!");
        }
        // Nếu là MANAGER -> Cho qua thoải mái, soi đơn nào cũng được!

        // --- 4. TRẢ VỀ KẾT QUẢ CHO FRONTEND ---
        return ResponseEntity.ok(response);
    }
}