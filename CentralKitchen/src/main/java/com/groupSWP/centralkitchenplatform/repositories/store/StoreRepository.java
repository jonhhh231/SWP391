package com.groupSWP.centralkitchenplatform.repositories.store;

import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, String> {

    Optional<Store> findByAccount_Username(String username);
}
