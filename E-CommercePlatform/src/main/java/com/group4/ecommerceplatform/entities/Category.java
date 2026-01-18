package com.group4.ecommerceplatform.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter

@Entity
@Table(name="Categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="Id")
    private int id;

    @Column(name="Name", nullable = false)
    private String name;

    @Column(name="Description", nullable = false)
    private String description;

    @Column(name="CreateAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;


    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Category(){
    }


    @OneToMany(
            mappedBy = "category",cascade = CascadeType.ALL,fetch =  FetchType.LAZY
    )
    private List<Product> products = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
