package com.groupSWP.centralkitchenplatform.entities.cart;

import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cart {

    @Id
    @Column(name = "cart_id")
    private String cartId;

    // Mỗi Store chỉ có đúng 1 cái giỏ hàng duy nhất
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", unique = true, nullable = false)
    private Store store;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}