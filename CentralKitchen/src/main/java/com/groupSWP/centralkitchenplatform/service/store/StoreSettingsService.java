package com.groupSWP.centralkitchenplatform.service.store;

import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileResponse;
import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileUpdateRequest;
import com.groupSWP.centralkitchenplatform.dto.store.StoreStatusRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.repositories.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Sử dụng Lombok để thay thế constructor thủ công
public class StoreSettingsService {

    private final StoreRepository storeRepository; // Đã đổi sang dùng StoreRepository chung

    public StoreProfileResponse getProfileByUsername(String username) {
        Store store = storeRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng cho user: " + username));

        return StoreProfileResponse.builder()
                .name(store.getName())
                .address(store.getAddress())
                .phone(store.getPhone())
                .isActive(store.isActive()) // Trả về thêm active theo yêu cầu của bạn
                .build();
    }

    @Transactional
    public void updateProfileByUsername(String username, StoreProfileUpdateRequest request) {
        Store store = storeRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng"));

        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());
        storeRepository.save(store);
    }

    // Sửa chữ 'username' thành 'storeId'
    @Transactional
    public void updateStatus(String storeId, Boolean isActive) {

        // 🌟 TÌM CỬA HÀNG THEO STORE ID (Chứ không tìm theo Username nữa)
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng với mã: " + storeId));

        store.setActive(isActive);
        storeRepository.save(store);
    }
}