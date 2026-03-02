package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreProfileRepository extends JpaRepository<Store, Long> {

    // Spring Data JPA sẽ tự động dịch tên hàm này thành query:
    // Tương đương: SELECT s FROM Store s WHERE s.account.username = ?1
    // Dấu "_" dùng để báo cho Spring biết cần chui vào trong object 'account' để tìm field 'username'
    Optional<Store> findByAccount_Username(String username);

}