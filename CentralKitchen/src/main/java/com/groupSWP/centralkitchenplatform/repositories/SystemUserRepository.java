package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, String> {

    // Tìm thằng nhân viên có mã lớn nhất thuộc Role này
    // Logic: Lọc theo Role -> Sắp xếp giảm dần -> Lấy thằng đầu tiên
    @Query("SELECT s.userId FROM SystemUser s WHERE s.role = :role ORDER BY s.userId DESC LIMIT 1")
    Optional<String> findLastUserIdByRole(@Param("role") SystemRole role);
}