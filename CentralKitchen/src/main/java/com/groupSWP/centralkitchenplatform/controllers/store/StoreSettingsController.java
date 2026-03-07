package com.groupSWP.centralkitchenplatform.controllers.store;

import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileResponse;
import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileUpdateRequest;
import com.groupSWP.centralkitchenplatform.dto.store.StoreStatusRequest;
import com.groupSWP.centralkitchenplatform.service.store.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/store/settings")
@RequiredArgsConstructor
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    /**
     * API: Xem hồ sơ cửa hàng
     * Primary Actor: STORE_MANAGER (Quản lý cửa hàng)
     * Secondary Actor: ADMIN, MANAGER (Quản trị hệ thống)
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN', 'MANAGER')") // 🔥 Đã sửa thành hasAnyRole
    public ResponseEntity<StoreProfileResponse> getStoreProfile(Principal principal) {
        return ResponseEntity.ok(storeSettingsService.getProfileByUsername(principal.getName()));
    }

    /**
     * API: Cập nhật thông tin cửa hàng (SĐT, Địa chỉ...)
     * Nếu muốn ADMIN cũng có quyền hỗ trợ sửa profile khi cần
     * Primary Actor: STORE_MANAGER
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN', 'MANAGER')") // 🔥 Đã sửa thành hasAnyRole
    public ResponseEntity<String> updateStoreProfile(Principal principal, @RequestBody StoreProfileUpdateRequest request) {
        storeSettingsService.updateProfileByUsername(principal.getName(), request);
        return ResponseEntity.ok("Cập nhật thông tin thành công!");
    }

    /**
     * API: Bật/Tắt trạng thái hoạt động (Mở/Đóng cửa trên Web/App)
     * Primary Actor: STORE_MANAGER
     */
    @PutMapping("/status")
    @PreAuthorize("hasRole('STORE_MANAGER')") // 🔥 Đã sửa thành hasRole
    public ResponseEntity<String> updateStatus(Principal principal, @RequestBody StoreStatusRequest request) {
        storeSettingsService.updateStatus(principal.getName(), request.getIsActive());
        String statusMsg = request.getIsActive() ? "MỞ CỬA" : "ĐÓNG CỬA";
        return ResponseEntity.ok("Trạng thái hiện tại: " + statusMsg);
    }
}