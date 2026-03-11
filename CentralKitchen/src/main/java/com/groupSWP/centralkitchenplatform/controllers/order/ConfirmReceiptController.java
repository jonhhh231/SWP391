package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptRequest;
import com.groupSWP.centralkitchenplatform.dto.order.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.service.order.ConfirmReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store")
public class ConfirmReceiptController {

    private final ConfirmReceiptService confirmReceiptService;

    /**
     * API: Xác nhận nhận hàng và cập nhật kho cửa hàng
     * Primary Actor: STORE_MANAGER (Quản lý cửa hàng Franchise)
     * Secondary Actor: ADMIN, MANAGER (Để xử lý hộ khi có sự cố)
     */

    @PatchMapping("/orders/{orderId}/confirm-receipt")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    public ConfirmReceiptResponse confirmReceipt(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "true") boolean updateStock,
            @RequestBody(required = false) ConfirmReceiptRequest request
    ) {
        String note = (request != null) ? request.getNote() : null;
        return confirmReceiptService.confirmReceipt(orderId, updateStock, note);
    }
}