package com.groupSWP.centralkitchenplatform.controllers.store;

import com.groupSWP.centralkitchenplatform.dto.store.StoreRequest;
import com.groupSWP.centralkitchenplatform.dto.store.StoreResponse;
import com.groupSWP.centralkitchenplatform.service.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @GetMapping("/all")
    public ResponseEntity<java.util.List<StoreResponse>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable String storeId,
            @RequestBody StoreRequest request
    ) {
        StoreResponse updatedStore = storeService.updateStore(storeId, request);
        return ResponseEntity.ok(updatedStore);
    }

    // API: Gán/Thay đổi Cửa hàng trưởng
    @PutMapping("/{storeId}/assign-manager")
    public ResponseEntity<String> assignManager(
            @PathVariable String storeId,
            @RequestParam UUID accountId) {
        return ResponseEntity.ok(storeService.changeStoreManager(storeId, accountId));
    }

    // API: Đóng cửa (Xóa mềm) Cửa hàng + Luân chuyển nhân sự
    @PutMapping("/{storeId}/status")
    public ResponseEntity<String> softDeleteStore(
            @PathVariable String storeId,
            @RequestParam(required = false) String transferToStoreId) { // Gửi thêm ID tiệm mới
        storeService.softDeleteStore(storeId, transferToStoreId);
        return ResponseEntity.ok("Đã đóng cửa hàng và luân chuyển nhân sự thành công!");
    }

//    @DeleteMapping("/{storeId}")
//    public ResponseEntity<String> deleteStore(@PathVariable String storeId) {
//        storeService.deleteStore(storeId);
//        return ResponseEntity.ok("Đã xóa cửa hàng và nhân viên liên quan thành công!");
//    }
}