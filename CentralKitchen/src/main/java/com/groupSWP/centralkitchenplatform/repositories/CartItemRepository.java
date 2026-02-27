package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.cart.CartItem;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItemKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, CartItemKey> {
    // Lấy toàn bộ món hàng đang có trong giỏ
    List<CartItem> findByCart_CartId(String cartId);

    // Mua xong thì phải dọn rác (xóa hết đồ trong giỏ)
    void deleteByCart_CartId(String cartId);
}