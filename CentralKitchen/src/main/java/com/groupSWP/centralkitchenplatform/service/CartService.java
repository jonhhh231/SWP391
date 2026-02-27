package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.cart.AddToCartRequest;
import com.groupSWP.centralkitchenplatform.dto.cart.CartResponse;
import com.groupSWP.centralkitchenplatform.dto.cart.CheckoutRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.cart.Cart;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItem;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItemKey;
import com.groupSWP.centralkitchenplatform.entities.config.SystemConfig;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItemKey;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SystemConfigRepository configRepository;

    // =======================================================
    // 1. LẤY HOẶC TẠO GIỎ HÀNG CHO CỬA HÀNG
    // =======================================================
    private Cart getOrCreateCart(String storeId) {
        return cartRepository.findByStore_StoreId(storeId).orElseGet(() -> {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Store ID: " + storeId));

            Cart newCart = Cart.builder()
                    .cartId("CART-" + storeId)
                    .store(store)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    // =======================================================
    // 2. THÊM MÓN VÀO GIỎ HÀNG
    // =======================================================
    @Transactional
    public void addToCart(String storeId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(storeId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        CartItemKey key = new CartItemKey(cart.getCartId(), product.getProductId());

        CartItem cartItem = cartItemRepository.findById(key).orElse(
                CartItem.builder()
                        .id(key)
                        .cart(cart)
                        .product(product)
                        .quantity(0)
                        .build()
        );

        // Cộng dồn số lượng
        cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        cartItemRepository.save(cartItem);

        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // =======================================================
    // 3. XEM GIỎ HÀNG
    // =======================================================
    // (Lát nữa rảnh Sếp code thêm hàm lấy danh sách items trả về CartResponse sau nhé,
    // ưu tiên làm xong luồng Checkout dưới đây trước).

    // =======================================================
    // 4. CHỐT ĐƠN (CHECKOUT) - NƠI RÀO CHẮN THỜI GIAN HOẠT ĐỘNG!
    // =======================================================
    @Transactional
    public Order checkoutCart(String storeId, CheckoutRequest request) {
        // 4.1. Kéo config từ Database lên (Nếu rỗng thì xài giờ mặc định)
        LocalTime startTime = parseConfigTime("ORDER_START_TIME", "08:00");
        LocalTime cutoffUrgent = parseConfigTime("CUTOFF_URGENT", "10:30");
        LocalTime cutoffStandard = parseConfigTime("CUTOFF_STANDARD", "14:00");

        LocalTime now = LocalTime.now();

        // 4.2. 🔴 RÀO CHẮN THỜI GIAN (TIME BARRIER) 🔴
        if (now.isBefore(startTime)) {
            throw new RuntimeException("Hệ thống đặt hàng chưa mở cửa. Vui lòng quay lại vào " + startTime);
        }

        if (request.getOrderType() == Order.OrderType.URGENT && now.isAfter(cutoffUrgent)) {
            throw new RuntimeException("Đã quá giờ (" + cutoffUrgent + ") đặt đơn GẤP (URGENT). Bếp không kịp chuẩn bị, vui lòng đặt đơn THƯỜNG!");
        }

        if (request.getOrderType() == Order.OrderType.STANDARD && now.isAfter(cutoffStandard)) {
            throw new RuntimeException("Đã quá giờ (" + cutoffStandard + ") đặt đơn THƯỜNG cho ngày mai. Vui lòng chờ đến 8h sáng mai để đặt!");
        }

        // 4.3. Lấy giỏ hàng ra xử lý
        Cart cart = cartRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống!"));

        // (Giả sử Sếp có hàm lấy CartItems từ Cart, hoặc dùng Query)
        // Vì trong Entity Cart chưa map OneToMany tới CartItem, mình query thẳng luôn:
        // List<CartItem> items = cartItemRepository.findByCart_CartId(cart.getCartId());
        // -> *Sếp nhớ bổ sung list CartItem vào Entity Cart hoặc viết Query trong repo nhé*

        throw new RuntimeException("Rào chắn thời gian đã OK! Sếp báo em để ráp code chuyển Cart -> Order nhé!");
    }

    // Hàm phụ hỗ trợ chuyển String ("14:00") từ DB thành LocalTime
    private LocalTime parseConfigTime(String key, String defaultTime) {
        return configRepository.findByConfigKey(key)
                .map(config -> LocalTime.parse(config.getConfigValue()))
                .orElse(LocalTime.parse(defaultTime));
    }
}