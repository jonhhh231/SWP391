package com.groupSWP.centralkitchenplatform.service.order;

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
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.order.CartRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.ProductRepository;
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
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OrderService orderService;

    // Helper: Tìm Store từ username
    private Store getStore(String username) {
        return accountRepository.findByUsername(username)
                .map(Account::getStore)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại hoặc chưa gán Cửa hàng!"));
    }

    // Helper: Lấy hoặc tạo Cart
    private Cart getOrCreateCart(Store store) {
        return cartRepository.findByStore_StoreId(store.getStoreId()).orElseGet(() ->
                cartRepository.save(Cart.builder()
                        .cartId("CART-" + store.getStoreId())
                        .store(store)
                        .lastUpdated(LocalDateTime.now())
                        .items(new ArrayList<>())
                        .build())
        );
    }

    @Transactional
    public void addToCart(String username, AddToCartRequest request) {
        Cart cart = getOrCreateCart(getStore(username));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        // Tìm xem sản phẩm đã có trong giỏ chưa
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = CartItem.builder()
                            .id(new CartItemKey(cart.getCartId(), product.getProductId()))
                            .cart(cart).product(product).quantity(0).build();
                    cart.getItems().add(newItem);
                    return newItem;
                });

        cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }

    public CartResponse getCart(String username) {
        Store store = getStore(username);
        Cart cart = getOrCreateCart(store);

        List<CartResponse.CartItemDto> itemDtos = cart.getItems().stream().map(item -> {
            BigDecimal subTotal = item.getProduct().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartResponse.CartItemDto.builder()
                    .productId(item.getProduct().getProductId())
                    .productName(item.getProduct().getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getProduct().getSellingPrice())
                    .subTotal(subTotal)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = itemDtos.stream().map(CartResponse.CartItemDto::getSubTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder().cartId(cart.getCartId()).storeId(store.getStoreId()).items(itemDtos).totalAmount(total).build();
    }

    @Transactional
    public OrderResponse checkoutCart(String username, CheckoutRequest request) {
        Store store = getStore(username);
        Cart cart = cartRepository.findByStore_StoreId(store.getStoreId())
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng đang trống!"));

        // Map sang OrderRequest (Sử dụng Constructor 2 tham số đã sửa ở bước trước)
        OrderRequest orderReq = new OrderRequest();
        orderReq.setStoreId(store.getStoreId());
        orderReq.setNote(request.getNote());
        orderReq.setItems(cart.getItems().stream().map(i ->
                new OrderRequest.OrderItemRequest(i.getProduct().getProductId(), i.getQuantity())
        ).collect(Collectors.toList()));

        OrderResponse response = orderService.createOrder(orderReq, request.getOrderType() == Order.OrderType.URGENT);

        // Xóa giỏ hàng sau khi checkout
        cart.getItems().clear();
        cartRepository.save(cart);

        return response;
    }

    @Transactional
    public void updateCartItem(String username, String productId, Integer newQuantity) {
        Cart cart = getOrCreateCart(getStore(username));

        // Nếu quantity <= 0 thì xóa luôn
        if (newQuantity <= 0) {
            removeCartItem(username, productId);
            return;
        }

        cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(newQuantity),
                        () -> { throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ!"); }
                );

        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // THÊM LẠI HÀM NÀY ĐỂ FIX LỖI Ở CONTROLLER
    @Transactional
    public void removeCartItem(String username, String productId) {
        Cart cart = getOrCreateCart(getStore(username));
        boolean removed = cart.getItems().removeIf(item -> item.getProduct().getProductId().equals(productId));

        if (!removed) {
            throw new RuntimeException("Sản phẩm không tồn tại trong giỏ hàng!");
        }

        cart.setLastUpdated(LocalDateTime.now());
        cartRepository.save(cart);
    }
}