package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.dto.cart.AddToCartRequest;
import com.groupSWP.centralkitchenplatform.dto.cart.CartResponse;
import com.groupSWP.centralkitchenplatform.dto.cart.CheckoutRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse; // Thêm dòng này để gọi đúng kiểu trả về
import com.groupSWP.centralkitchenplatform.service.order.CartService;
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
            String username = principal.getName(); // Lấy từ Token (VD: "ST_002")
            log.info("User {} đang thêm {} món {} vào giỏ hàng", username, request.getQuantity(), request.getProductId());

            cartService.addToCart(username, request);

            return ResponseEntity.ok("Đã thêm sản phẩm vào giỏ hàng thành công!");
        } catch (RuntimeException e) {
            log.error("Lỗi khi thêm vào giỏ hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =======================================================
    // 2. API CHỐT ĐƠN (CHECKOUT TỪ GIỎ NHÁP)
    // =======================================================
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            Principal principal,
            @RequestBody CheckoutRequest request) {
        try {
            String username = principal.getName();
            log.info("User {} yêu cầu chốt đơn loại {}", username, request.getOrderType());

            // FIX ĐỎ Ở ĐÂY: Hứng kết quả bằng OrderResponse thay vì Order thô
            OrderResponse createdOrder = cartService.checkoutCart(username, request);

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

    // =======================================================
    // 4. API CẬP NHẬT SỐ LƯỢNG MÓN
    // =======================================================
    @PutMapping("/update")
    public ResponseEntity<?> updateCartItem(
            Principal principal,
            @RequestBody AddToCartRequest request) {
        try {
            String username = principal.getName();
            log.info("User {} sửa số lượng món {} thành {}", username, request.getProductId(), request.getQuantity());

            cartService.updateCartItem(username, request.getProductId(), request.getQuantity());

            return ResponseEntity.ok("Đã cập nhật số lượng thành công!");
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật giỏ hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =======================================================
    // 5. API XÓA MÓN KHỎI GIỎ HÀNG
    // =======================================================
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeCartItem(
            Principal principal,
            @PathVariable String productId) {
        try {
            String username = principal.getName();
            log.info("User {} xóa món {} khỏi giỏ hàng", username, productId);

            cartService.removeCartItem(username, productId);

            return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng!");
        } catch (RuntimeException e) {
            log.error("Lỗi khi xóa món: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}