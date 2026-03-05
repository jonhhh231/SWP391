package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptRequest;
import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.service.order.ConfirmReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store")
public class ConfirmReceiptController {

    private final ConfirmReceiptService confirmReceiptService;

    /**
     * PATCH /api/store/orders/{orderId}/confirm-receipt
     * Cửa hàng xác nhận nhận hàng từ Bếp Trung Tâm
     * Role: STORE_MANAGER, ADMIN
     */
    @PatchMapping("/orders/{orderId}/confirm-receipt")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')") // Thêm áo giáp bảo mật
    public ResponseEntity<ConfirmReceiptResponse> confirmReceipt( // Bọc ResponseEntity cho chuẩn
        @PathVariable String orderId,
        @RequestParam(defaultValue = "true") boolean updateStock,
        @RequestBody(required = false) ConfirmReceiptRequest request
    ) {
        String note = (request != null) ? request.getNote() : null;

        // Gọi Service và trả về 200 OK
        ConfirmReceiptResponse response = confirmReceiptService.confirmReceipt(orderId, updateStock, note);
        return ResponseEntity.ok(response);
    }
}