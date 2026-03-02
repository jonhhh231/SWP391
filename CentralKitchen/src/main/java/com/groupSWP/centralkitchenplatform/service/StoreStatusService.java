package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.store.StoreStatusRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.repositories.StoreRepository;
import org.springframework.stereotype.Service;

@Service
public class StoreStatusService {

    private final StoreRepository storeRepository;

    public StoreStatusService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public void updateStatus(String username, StoreStatusRequest request) {
        // Tùy vào hàm tìm kiếm có sẵn trong StoreRepository trên nhánh main của bạn
        // Giả sử là findByAccount_Username hoặc findByUsername
        Store store = storeRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng cho user: " + username));

        store.setActive(request.getIsActive()); // hoặc setIsActive tùy entity của bạn
        storeRepository.save(store);
    }
}