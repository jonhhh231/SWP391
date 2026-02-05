package com.groupSWP.centralkitchenplatform.entities.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", columnDefinition = "BINARY(16)")
    private UUID accountId;

    @Column(unique = true)
    private String username;
    private String password; // Mật khẩu CHỈ để ở đây

    private String role; // Role để filter nhanh ở JWT

    // Liên kết 1-1 với thông tin chi tiết người dùng
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonIgnore
    private SystemUser systemUser;

    @Column(name = "is_active")
    private boolean isActive = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", unique = true)
    private Store store;
}