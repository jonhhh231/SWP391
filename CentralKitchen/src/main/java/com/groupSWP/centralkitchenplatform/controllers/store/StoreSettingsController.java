package com.groupSWP.centralkitchenplatform.controllers.store;

import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileResponse;
import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileUpdateRequest;
import com.groupSWP.centralkitchenplatform.dto.store.StoreStatusRequest;
import com.groupSWP.centralkitchenplatform.service.store.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/store/settings")
@RequiredArgsConstructor
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    @GetMapping("/profile")
    public ResponseEntity<StoreProfileResponse> getStoreProfile(Principal principal) {
        return ResponseEntity.ok(storeSettingsService.getProfileByUsername(principal.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateStoreProfile(Principal principal, @RequestBody StoreProfileUpdateRequest request) {
        storeSettingsService.updateProfileByUsername(principal.getName(), request);
        return ResponseEntity.ok("Cập nhật thông tin thành công!");
    }

    @PutMapping("/status")
    public ResponseEntity<String> updateStatus(Principal principal, @RequestBody StoreStatusRequest request) {
        storeSettingsService.updateStatus(principal.getName(), request.getIsActive());
        String statusMsg = request.getIsActive() ? "MỞ CỬA" : "ĐÓNG CỬA";
        return ResponseEntity.ok("Trạng thái hiện tại: " + statusMsg);
    }
}