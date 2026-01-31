package com.groupSWP.centralkitchenplatform.entities.auth;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", columnDefinition = "BINARY(16)") // MySQL lưu dạng binary để đạt hiệu năng tốt nhất
    private UUID accountId;

    @Column(unique = true)
    private String username;
    private String password;
    private String role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", unique = true)
    private Store store;
}