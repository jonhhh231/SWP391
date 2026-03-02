package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileResponse;
import com.groupSWP.centralkitchenplatform.dto.store.StoreProfileUpdateRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.repositories.StoreProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class StoreSettingsService {

    private final StoreProfileRepository storeProfileRepository;

    public StoreSettingsService(StoreProfileRepository storeProfileRepository) {
        this.storeProfileRepository = storeProfileRepository;
    }

    public StoreProfileResponse getProfileByUsername(String username) {
        // Sử dụng hàm không có @Query
        Store store = storeProfileRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng cho user: " + username));

        return StoreProfileResponse.builder()
                .name(store.getName())
                .address(store.getAddress())
                .phone(store.getPhone())
                .build();
    }

    public void updateProfileByUsername(String username, StoreProfileUpdateRequest request) {
        // Sử dụng hàm không có @Query
        Store store = storeProfileRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng cho user: " + username));

        // Chỉ cập nhật 3 trường được phép: tên, địa chỉ, số điện thoại
        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());

        // Lưu lại xuống DB (Không cập nhật store_id hay type)
        storeProfileRepository.save(store);
    }
}