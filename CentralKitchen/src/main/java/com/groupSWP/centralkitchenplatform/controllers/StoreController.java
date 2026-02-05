package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.store.StoreRequest;
import com.groupSWP.centralkitchenplatform.dto.store.StoreResponse;
import com.groupSWP.centralkitchenplatform.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@RequestBody StoreRequest request) {
        // Gọi service xử lý
        StoreResponse newStore = storeService.createStore(request);

        // Trả về kết quả 200 OK
        return ResponseEntity.ok(newStore);
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable String storeId,
            @RequestBody StoreRequest request
    ) {
        StoreResponse updatedStore = storeService.updateStore(storeId, request);
        return ResponseEntity.ok(updatedStore);
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<String> deleteStore(@PathVariable String storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.ok("Đã xóa cửa hàng và nhân viên liên quan thành công!");
    }
}