package com.group4.ecommerceplatform.repository;

import com.group4.ecommerceplatform.entities.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = ?1 WHERE p.id = ?2")
    void updatePriceOnly(double newPrice, UUID productId); // int -> UUID

    @Query
    List<Product> findAllByOrderByPriceDesc();

    @Query
    List<Product> findByNameContainingIgnoreCase(String name);



}
