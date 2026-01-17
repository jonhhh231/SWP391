package com.group4.ecommerceplatform.Entity;

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

    @Column(name="Create_Time", nullable = false)
    private LocalDateTime created_at;

    @Column(name="Update_Time", nullable = false)
    private LocalDateTime updated_at;


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
        created_at = LocalDateTime.now();
        updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updated_at = LocalDateTime.now();
    }
}
