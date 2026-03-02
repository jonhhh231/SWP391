package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.cart.AddToCartRequest;
import com.groupSWP.centralkitchenplatform.dto.cart.CartResponse;
import com.groupSWP.centralkitchenplatform.dto.cart.CheckoutRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.cart.Cart;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItem;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItemKey;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;

    // 🛡️ Bơm OrderService vào để "ký gửi" việc tạo đơn
    private final OrderService orderService;

    // =======================================================
    // HÀM HELPER: TÌM CỬA HÀNG TỪ USERNAME ĐĂNG NHẬP
    // =======================================================
    private Store getStoreByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống!"));

        Store store = account.getStore();
        if (store == null) {
            throw new RuntimeException("Tài khoản này chưa được cấp quyền quản lý Cửa hàng nào!");
        }
        return store;
    }

    // =======================================================
    // 1. LẤY HOẶC TẠO GIỎ HÀNG CHO CỬA HÀNG (ĐƠN NHÁP)
    // =======================================================
    private Cart getOrCreateCart(Store store) {
        return cartRepository.findByStore_StoreId(store.getStoreId()).orElseGet(() -> {
            Cart newCart = Cart.builder()
                    .cartId("CART-" + store.getStoreId())
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
    public void addToCart(String username, AddToCartRequest request) {
        Store store = getStoreByUsername(username);
        Cart cart = getOrCreateCart(store);

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

        cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        cartItemRepository.save(cartItem);

        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // =======================================================
    // 3. XEM GIỎ HÀNG (VIEW CART)
    // =======================================================
    public CartResponse getCart(String username) {
        Store store = getStoreByUsername(username);
        Cart cart = getOrCreateCart(store);
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        List<CartResponse.CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            BigDecimal subTotal = product.getSellingPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(subTotal);

            itemDtos.add(CartResponse.CartItemDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(product.getSellingPrice())
                    .subTotal(subTotal)
                    .build());
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .storeId(store.getStoreId())
                .items(itemDtos)
                .totalAmount(totalAmount)
                .build();
    }

    // =======================================================
    // 4. CHỐT ĐƠN TỪ GIỎ HÀNG (ỦY QUYỀN CHO ORDER SERVICE)
    // =======================================================
    @Transactional
    public OrderResponse checkoutCart(String username, CheckoutRequest request) {
        Store store = getStoreByUsername(username);

        // 4.1. Lấy giỏ hàng ra
        Cart cart = cartRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng đang trống, không có gì để chốt!"));

        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Chưa có món nào trong giỏ hàng cả Sếp ơi!");
        }

        // 4.2. ÉP KIỂU TỪ GIỎ HÀNG SANG ORDER REQUEST
        OrderRequest orderReq = new OrderRequest();
        orderReq.setStoreId(store.getStoreId());
        orderReq.setNote(request.getNote());

        // LƯU Ý: Nếu CheckoutRequest của Sếp có trường deliveryWindow thì get vào đây,
        // không có thì truyền null cũng không sao vì OrderService đã có logic lo vụ này
        // orderReq.setDeliveryWindow(request.getDeliveryWindow());

        List<OrderRequest.OrderItemRequest> itemReqs = cartItems.stream().map(cItem -> {
            OrderRequest.OrderItemRequest i = new OrderRequest.OrderItemRequest();
            i.setProductId(cItem.getProduct().getProductId());
            i.setQuantity(cItem.getQuantity());
            return i;
        }).collect(Collectors.toList());

        orderReq.setItems(itemReqs);

        // 4.3. CHUYỂN PHÁT NHANH SANG ORDER SERVICE XỬ LÝ (Tận dụng rào chắn ERP)
        boolean isUrgent = (request.getOrderType() == Order.OrderType.URGENT);
        OrderResponse response = orderService.createOrder(orderReq, isUrgent);

        // 4.4. DỌN SẠCH GIỎ HÀNG SAU KHI CHỐT THÀNH CÔNG
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        log.info("Cửa hàng {} đã chốt Giỏ hàng thành Đơn thật thành công! Mã đơn: {}", store.getStoreId(), response.getOrderId());

        return response;
    }

    // =======================================================
    // 5. CẬP NHẬT SỐ LƯỢNG MÓN TRONG GIỎ
    // =======================================================
    @Transactional
    public void updateCartItem(String username, String productId, Integer newQuantity) {
        Store store = getStoreByUsername(username);
        Cart cart = getOrCreateCart(store);

        CartItemKey key = new CartItemKey(cart.getCartId(), productId);
        CartItem cartItem = cartItemRepository.findById(key)
                .orElseThrow(() -> new RuntimeException("Sản phẩm này không có trong giỏ hàng!"));

        if (newQuantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        }

        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // =======================================================
    // 6. XÓA HẲN MÓN KHỎI GIỎ HÀNG
    // =======================================================
    @Transactional
    public void removeCartItem(String username, String productId) {
        Store store = getStoreByUsername(username);
        Cart cart = getOrCreateCart(store);

        CartItemKey key = new CartItemKey(cart.getCartId(), productId);
        CartItem cartItem = cartItemRepository.findById(key)
                .orElseThrow(() -> new RuntimeException("Sản phẩm này không có trong giỏ hàng!"));

        cartItemRepository.delete(cartItem);

        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }
}