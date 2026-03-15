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
     * API Xem hồ sơ thiết lập Cửa hàng.
     * <p>Primary Actor: STORE_MANAGER | Secondary Actor: ADMIN, MANAGER.</p>
     *
     * @param principal Đối tượng bảo mật chứa danh tính người gọi.
     * @return Phản hồi HTTP 200 chứa thông tin profile của cửa hàng.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    public ResponseEntity<StoreProfileResponse> getStoreProfile(Principal principal) {
        return ResponseEntity.ok(storeSettingsService.getProfileByUsername(principal.getName()));
    }

    /**
     * API Cập nhật thông tin hồ sơ Cửa hàng (SĐT, Địa chỉ...).
     * <p>Cho phép Cửa hàng trưởng tự cập nhật hoặc Admin hỗ trợ chỉnh sửa.</p>
     *
     * @param principal Đối tượng bảo mật chứa danh tính người gọi.
     * @param request   Payload chứa dữ liệu hồ sơ mới.
     * @return Phản hồi HTTP 200 xác nhận cập nhật thành công.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    public ResponseEntity<String> updateStoreProfile(
            Principal principal,
            @RequestBody StoreProfileUpdateRequest request) {
        storeSettingsService.updateProfileByUsername(principal.getName(), request);
        return ResponseEntity.ok("Cập nhật thông tin thành công!");
    }

    /**
     * API Bật/Tắt trạng thái hoạt động của Cửa hàng.
     * <p>Nghiệp vụ Mở/Đóng cửa được bảo mật nghiêm ngặt và chỉ ADMIN mới có quyền thực thi.</p>
     *
     * @param storeId Mã định danh Cửa hàng.
     * @param request Payload chứa cờ trạng thái isActive (boolean).
     * @return Phản hồi HTTP 200 thông báo trạng thái hiện tại của cửa hàng.
     */
    @PutMapping("/{storeId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStatus(
            @PathVariable String storeId,
            @RequestBody StoreStatusRequest request) {

        storeSettingsService.updateStatus(storeId, request.getIsActive());

        String statusMsg = request.getIsActive() ? "MỞ CỬA" : "ĐÓNG CỬA";
        return ResponseEntity.ok("Cửa hàng " + storeId + " trạng thái hiện tại: " + statusMsg);
    }
}