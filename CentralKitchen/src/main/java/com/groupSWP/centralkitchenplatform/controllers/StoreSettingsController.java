package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileResponse;
import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileUpdateRequest;
import com.groupSWP.centralkitchenplatform.service.StoreSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/store/settings/profile")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    // API: Xem thông tin cửa hàng (Store Profile)
    @GetMapping
    public ResponseEntity<StoreProfileResponse> getStoreProfile(Principal principal) {
        // Lấy username từ token của người dùng đang đăng nhập (Principal)
        String username = principal.getName();

        // Gọi service lấy thông tin trả về
        StoreProfileResponse profile = storeSettingsService.getProfileByUsername(username);

        return ResponseEntity.ok(profile);
    }

    // API: Cập nhật thông tin cửa hàng (Store Profile)
    @PutMapping
    public ResponseEntity<String> updateStoreProfile(
            Principal principal,
            @RequestBody StoreProfileUpdateRequest request) {

        // Lấy username từ token
        String username = principal.getName();

        // Gọi service cập nhật thông tin (chỉ cập nhật name, address, phone)
        storeSettingsService.updateProfileByUsername(username, request);

        return ResponseEntity.ok("Cập nhật thông tin cửa hàng thành công!");
    }
}