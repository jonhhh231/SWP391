package com.groupSWP.centralkitchenplatform.repositories.auth;

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

    // Tìm kiếm theo tên gần đúng (không phân biệt hoa thường) và loại trừ ADMIN
    @Query("SELECT a FROM Account a JOIN a.systemUser u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND a.role != 'ADMIN'")
    List<Account> searchByFullNameExcludingAdmin(@Param("keyword") String keyword);

    // ======================================================
    // 🌟 TÌM NHÂN VIÊN "DỰ BỊ" (ĐANG RẢNH VIỆC) ĐỂ LÀM NGƯỜI THẾ CHỖ
    // Điều kiện: Đang Active + Là STORE_MANAGER + Chưa ôm tiệm nào (store IS NULL)
    // ======================================================
    @Query("SELECT a FROM Account a WHERE a.isActive = true AND a.role = 'STORE_MANAGER' AND a.store IS NULL")
    List<Account> findFreeStoreManagers();
}
