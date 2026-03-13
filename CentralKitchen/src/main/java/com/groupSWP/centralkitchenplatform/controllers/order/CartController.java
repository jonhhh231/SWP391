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

/**
 * Controller quản lý luồng Giỏ hàng (Cart) dành riêng cho Cửa hàng trưởng.
 * <p>
 * Lớp này xử lý các thao tác tương tác với giỏ hàng nháp trước khi chốt thành đơn đặt hàng (Order) chính thức.
 * Các chức năng bao gồm: Thêm món, xem giỏ hàng, cập nhật số lượng, xóa món và thanh toán (Checkout).
 * Mọi thao tác đều được gắn liền với danh tính người dùng (thông qua {@link Principal}).
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/store/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // =======================================================
    // 1. API THÊM MÓN VÀO GIỎ HÀNG
    // =======================================================
    /**
     * API Thêm sản phẩm vào giỏ hàng.
     * <p>Nhận thông tin mã sản phẩm và số lượng từ client, sau đó lưu vào giỏ hàng nháp của người dùng.</p>
     *
     * @param principal "Thẻ căn cước" trích xuất từ Token, chứa username của Cửa hàng trưởng.
     * @param request   Payload chứa productId và quantity cần thêm.
     * @return Phản hồi HTTP 200 kèm thông báo thành công hoặc 400 nếu có lỗi nghiệp vụ (hết hàng, mã sai...).
     */
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
    /**
     * API Chốt đơn hàng (Checkout).
     * <p>
     * Chuyển đổi toàn bộ dữ liệu từ giỏ hàng nháp hiện tại thành một Đơn đặt hàng (Order) chính thức,
     * đồng thời xóa/làm sạch giỏ hàng sau khi chốt thành công.
     * </p>
     *
     * @param principal Danh tính của Cửa hàng trưởng đang thao tác.
     * @param request   Payload chứa các cấu hình đơn hàng (như loại đơn, ghi chú...).
     * @return Phản hồi HTTP 200 chứa đối tượng {@link OrderResponse} (Thông tin đơn hàng vừa tạo).
     */
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
    /**
     * API Xem chi tiết giỏ hàng hiện tại.
     * <p>Truy xuất danh sách các món đang nằm trong giỏ hàng cùng với tổng số lượng, tổng tiền (nếu có).</p>
     *
     * @param principal Danh tính người dùng.
     * @return Phản hồi HTTP 200 chứa đối tượng {@link CartResponse} với danh sách item bên trong.
     */
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
    /**
     * API Cập nhật số lượng của một món hàng đã có trong giỏ.
     * <p>Dùng khi Cửa hàng trưởng muốn tăng/giảm số lượng trực tiếp trong màn hình xem giỏ hàng.</p>
     *
     * @param principal Danh tính người dùng.
     * @param request   Payload tái sử dụng lại AddToCartRequest chứa productId và quantity mới.
     * @return Phản hồi HTTP 200 kèm thông báo đã cập nhật thành công.
     */
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
    /**
     * API Xóa một sản phẩm cụ thể khỏi giỏ hàng.
     *
     * @param principal Danh tính người dùng.
     * @param productId Mã sản phẩm cần loại bỏ khỏi giỏ.
     * @return Phản hồi HTTP 200 kèm thông báo xóa thành công.
     */
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