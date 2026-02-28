package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUsername(String username);
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.systemUser WHERE a.role != 'ADMIN'")
    List<Account> findAllExcludingAdmin();
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.systemUser WHERE a.role != 'ADMIN' AND a.isActive = :isActive")
    List<Account> findByIsActiveExcludingAdmin(@Param("isActive") boolean isActive);
}
