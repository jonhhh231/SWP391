package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, String> {
    // Lưu ý: ID của SystemUser là String (userId), nên tham số thứ 2 phải là String nha Sếp
}