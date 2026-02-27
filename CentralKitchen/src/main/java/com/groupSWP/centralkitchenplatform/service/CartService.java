package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.cart.AddToCartRequest;
import com.groupSWP.centralkitchenplatform.dto.cart.CartResponse;
import com.groupSWP.centralkitchenplatform.dto.cart.CheckoutRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.cart.Cart;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItem;
import com.groupSWP.centralkitchenplatform.entities.cart.CartItemKey;
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
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SystemConfigRepository configRepository;

    // 🛡️ VŨ KHÍ MỚI: Dùng để check tài khoản
    private final AccountRepository accountRepository;

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
    // 1. LẤY HOẶC TẠO GIỎ HÀNG CHO CỬA HÀNG
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
        Store store = getStoreByUsername(username); // Xác thực Store trước
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
        // 1. Dùng Helper lấy Store chuẩn xác từ Username
        Store store = getStoreByUsername(username);

        // 2. Tìm giỏ hàng (nếu ông này chưa từng mua gì thì nó tự đẻ ra 1 cái giỏ rỗng)
        Cart cart = getOrCreateCart(store);

        // 3. Lôi hết đồ trong giỏ ra
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        List<CartResponse.CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 4. Lặp qua từng món để tính tiền
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Tính thành tiền = Giá bán * Số lượng
            BigDecimal subTotal = product.getSellingPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(subTotal);

            // Gói gọn vào DTO để trả về cho Frontend
            itemDtos.add(CartResponse.CartItemDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(product.getSellingPrice())
                    .subTotal(subTotal)
                    .build());
        }

        // 5. Gom tất cả lại trả về
        return CartResponse.builder()
                .cartId(cart.getCartId())
                .storeId(store.getStoreId())
                .items(itemDtos)
                .totalAmount(totalAmount)
                .build();
    }

    // =======================================================
    // 4. CHỐT ĐƠN (CHECKOUT)
    // =======================================================
    @Transactional
    public Order checkoutCart(String username, CheckoutRequest request) {
        Store store = getStoreByUsername(username); // Xác thực Store

        // 4.1. Kéo config từ Database lên
        LocalTime startTime = parseConfigTime("ORDER_START_TIME", "08:00");
        LocalTime cutoffUrgent = parseConfigTime("CUTOFF_URGENT", "10:30");
        LocalTime cutoffStandard = parseConfigTime("CUTOFF_STANDARD", "14:00");

        LocalTime now = LocalTime.now();

        // 4.2. 🔴 RÀO CHẮN THỜI GIAN
        if (now.isBefore(startTime)) {
            throw new RuntimeException("Hệ thống đặt hàng chưa mở cửa. Vui lòng quay lại vào " + startTime);
        }

        if (request.getOrderType() == Order.OrderType.URGENT && now.isAfter(cutoffUrgent)) {
            throw new RuntimeException("Đã quá giờ (" + cutoffUrgent + ") đặt đơn GẤP. Bếp không kịp chuẩn bị, vui lòng đặt đơn THƯỜNG!");
        }

        if (request.getOrderType() == Order.OrderType.STANDARD && now.isAfter(cutoffStandard)) {
            throw new RuntimeException("Đã quá giờ (" + cutoffStandard + ") đặt đơn THƯỜNG cho ngày mai. Vui lòng chờ đến 8h sáng mai để đặt!");
        }

        // 4.3. Lấy giỏ hàng ra xử lý
        Cart cart = cartRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng!"));

        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể chốt đơn!");
        }

        // 4.4. Tạo Đơn hàng mới (Order)
        Order newOrder = new Order();
        newOrder.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        newOrder.setStore(store);
        newOrder.setStatus(Order.OrderStatus.NEW);
        newOrder.setOrderType(request.getOrderType());
        newOrder.setNote(request.getNote());

        // Phụ thu URGENT
        if (request.getOrderType() == Order.OrderType.URGENT) {
            newOrder.setSurcharge(new BigDecimal("50000"));
        } else {
            newOrder.setSurcharge(BigDecimal.ZERO);
        }

        Order savedOrder = orderRepository.save(newOrder);

        // 4.5. Chuyển CartItem -> OrderItem
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cItem : cartItems) {
            Product product = cItem.getProduct();

            OrderItem oItem = new OrderItem();
            oItem.setId(new OrderItemKey(savedOrder.getOrderId(), product.getProductId()));
            oItem.setOrder(savedOrder);
            oItem.setProduct(product);
            oItem.setQuantity(cItem.getQuantity());
            oItem.setPriceAtOrder(product.getSellingPrice());

            orderItems.add(oItem);

            BigDecimal lineTotal = product.getSellingPrice().multiply(new BigDecimal(cItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        orderItemRepository.saveAll(orderItems);

        savedOrder.setTotalAmount(totalAmount.add(savedOrder.getSurcharge()));
        orderRepository.save(savedOrder);

        // 4.6. Dọn dẹp giỏ hàng
        cartItemRepository.deleteByCart_CartId(cart.getCartId());

        log.info("Cửa hàng {} đã chốt đơn thành công! Mã đơn: {}", store.getStoreId(), savedOrder.getOrderId());
        return savedOrder;
    }

    private LocalTime parseConfigTime(String key, String defaultTime) {
        return configRepository.findByConfigKey(key)
                .map(config -> LocalTime.parse(config.getConfigValue()))
                .orElse(LocalTime.parse(defaultTime));
    }
}