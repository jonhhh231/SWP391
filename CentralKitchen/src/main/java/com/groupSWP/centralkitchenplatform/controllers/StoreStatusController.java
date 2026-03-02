package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.store.StoreStatusRequest;
import com.groupSWP.centralkitchenplatform.service.StoreStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/store/settings/status")
public class StoreStatusController {

    private final StoreStatusService storeStatusService;

    public StoreStatusController(StoreStatusService storeStatusService) {
        this.storeStatusService = storeStatusService;
    }

    @PutMapping
    public ResponseEntity<String> updateStoreStatus(
            Principal principal,
            @RequestBody StoreStatusRequest request) {

        String username = principal.getName();
        storeStatusService.updateStatus(username, request);

        String message = (request.getIsActive() != null && request.getIsActive())
                ? "MỞ CỬA (Nhận đơn bình thường)"
                : "ĐÓNG CỬA (Tạm ngưng nhận đơn)";

        return ResponseEntity.ok("Đã cập nhật trạng thái: " + message);
    }
}