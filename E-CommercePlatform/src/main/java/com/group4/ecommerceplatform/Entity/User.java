package com.group4.ecommerceplatform.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    @Column(name = "Id", columnDefinition = "uniqueidentifier")
    private UUID id;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "PhoneNumber", nullable = false)
    private String phoneNumber;

    @Column(name = "Address", nullable = false)
    private String address;

    @Column(name = "Role", nullable = false)
    private String role;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive;

    @Column(name = "Create_at", nullable = false)
    private LocalDateTime created_at;

    @Column(name = "Updated_at", nullable = false)
    private LocalDateTime updated_at;

    @PrePersist
    public void onCreate() {
        created_at = LocalDateTime.now();
        updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updated_at = LocalDateTime.now();
    }


    public User() {
    }

    public User(
            String fullName,
            String email,
            String password,
            String phone_number,
            String address,
            String role
    ) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phone_number;
        this.address = address;
        this.role = role;
        this.isActive = true;
    }

}
