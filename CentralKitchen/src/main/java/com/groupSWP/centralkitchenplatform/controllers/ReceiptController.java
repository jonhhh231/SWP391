package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.ConfirmReceipt.ConfirmReceiptRequest;
import com.groupSWP.centralkitchenplatform.dto.ConfirmReceipt.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<ConfirmReceiptResponse> confirmReceipt(
            @PathVariable String orderId,
            @RequestParam String storeId,
            @RequestBody(required = false) ConfirmReceiptRequest req
    ) {
        return ResponseEntity.ok(receiptService.confirmReceipt(orderId, storeId, req));
    }
}