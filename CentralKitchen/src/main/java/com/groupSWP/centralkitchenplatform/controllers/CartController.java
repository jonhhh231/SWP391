package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.cart.AddToCartRequest;
import com.groupSWP.centralkitchenplatform.dto.cart.CartResponse;
import com.groupSWP.centralkitchenplatform.dto.cart.CheckoutRequest;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/store/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // =======================================================
    // 1. API THÊM MÓN VÀO GIỎ HÀNG
    // =======================================================
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            Principal principal,
            @RequestBody AddToCartRequest request) {
        try {
            String username = principal.getName(); // Lấy từ Token (VD: "store_q1")
            log.info("User {} đang thêm {} món {} vào giỏ hàng", username, request.getQuantity(), request.getProductId());

            cartService.addToCart(username, request);

            return ResponseEntity.ok("Đã thêm sản phẩm vào giỏ hàng thành công!");
        } catch (RuntimeException e) {
            log.error("Lỗi khi thêm vào giỏ hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =======================================================
    // 2. API CHỐT ĐƠN (CHECKOUT)
    // =======================================================
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            Principal principal,
            @RequestBody CheckoutRequest request) {
        try {
            String username = principal.getName();
            log.info("User {} yêu cầu chốt đơn loại {}", username, request.getOrderType());

            // Gọi Service - Nếu lố giờ Cut-off nó sẽ tự văng Exception, Controller bắt được sẽ ném ra 400 Bad Request
            Order createdOrder = cartService.checkoutCart(username, request);

            return ResponseEntity.ok(createdOrder);
        } catch (RuntimeException e) {
            log.error("Lỗi khi chốt đơn: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =======================================================
    // 3. API XEM GIỎ HÀNG
    // =======================================================
    @GetMapping
    public ResponseEntity<?> getCart(Principal principal) {
        try {
            String username = principal.getName();
            log.info("User {} đang xem giỏ hàng", username);

            CartResponse response = cartService.getCart(username);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Lỗi khi xem giỏ hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}