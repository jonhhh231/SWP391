package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.order.OrderDetailResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderHistoryResponse;
import com.groupSWP.centralkitchenplatform.dto.order.OrderRequest;
import com.groupSWP.centralkitchenplatform.dto.order.OrderResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItemKey;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import com.groupSWP.centralkitchenplatform.repositories.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.ProductRepository;
import com.groupSWP.centralkitchenplatform.repositories.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductionService productionService;

    // =========================================================================
    // HÀM TẠO ĐƠN TRỰC TIẾP "ALL IN ONE" (TÍCH HỢP RÀO CHẮN ERP)
    // =========================================================================
    @Transactional
    public OrderResponse createOrder(OrderRequest request, boolean isUrgent) {

        // --- 🛡️ 0. BẢO VỆ: CHỐNG ĐƠN HÀNG RỖNG ---
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống! Sếp vui lòng chọn ít nhất 1 món trước khi chốt đơn nhé!");
        }

        // --- ⚙️ 1. CẤU HÌNH ERP CHUẨN ---
        LocalTime now = LocalTime.now();
        LocalTime OPEN_TIME = LocalTime.of(8, 0);
        LocalTime URGENT_CUTOFF = LocalTime.of(10, 0);   // Gấp chốt 10h30
        LocalTime STANDARD_CUTOFF = LocalTime.of(13, 0); // Thường chốt 13h00
        BigDecimal URGENT_SURCHARGE = new BigDecimal("100000"); // 100k phụ phí

        // --- 🛑 2. RÀO CHẮN THỜI GIAN ---
        if (now.isBefore(OPEN_TIME)) {
            throw new RuntimeException("Hệ thống chưa mở cửa (8:00 AM mới nhận đơn nha Sếp)!");
        }

        if (isUrgent && now.isAfter(URGENT_CUTOFF)) {
            throw new RuntimeException("Đã quá 10:30 AM, Bếp ngưng nhận đơn GẤP rồi ạ!");
        }

        if (!isUrgent && now.isAfter(STANDARD_CUTOFF)) {
            throw new RuntimeException("Đã quá 13:00 PM, vui lòng chờ mai đặt đơn THƯỜNG Sếp nhé!");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại: " + request.getStoreId()));

        Order order = new Order();
        order.setStore(store);
        order.setStatus(Order.OrderStatus.NEW);

        String deliveryWin = request.getDeliveryWindow() != null ? request.getDeliveryWindow().toUpperCase() : "MORNING";
        order.setDeliveryWindow(Order.DeliveryWindow.valueOf(deliveryWin));

        order.setNote(request.getNote());

        // --- 💰 3. PHÂN LOẠI & TÍNH PHỤ PHÍ ---
        BigDecimal surcharge = BigDecimal.ZERO;
        String prefix = "STD";
        if (isUrgent) {
            order.setOrderType(Order.OrderType.URGENT);
            surcharge = URGENT_SURCHARGE;
            prefix = "URG";
        } else {
            order.setOrderType(Order.OrderType.STANDARD);
        }
        order.setSurcharge(surcharge);
        order.setOrderId(generateSmartOrderId(prefix, store.getStoreId()));

        // --- ⚡ 4. TỐI ƯU HIỆU NĂNG (CHỐNG N+1 QUERY) ---
        List<String> productIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.getProductId());
            if (product == null) throw new RuntimeException("Sản phẩm không hợp lệ: " + itemReq.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setId(new OrderItemKey(order.getOrderId(), product.getProductId()));
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPriceAtOrder(product.getSellingPrice());

            totalAmount = totalAmount.add(product.getSellingPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            orderItems.add(orderItem);
        }

        // --- 💾 5. LƯU DATABASE ---
        order.setTotalAmount(totalAmount.add(surcharge));
        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // --- 📦 6. MAPPING TRẢ VỀ ---
        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .status(savedOrder.getStatus().name())
                .totalAmount(savedOrder.getTotalAmount())
                .message(isUrgent ? "Tạo đơn KHẨN CẤP thành công (+100k phí)!" : "Tạo đơn TIÊU CHUẨN thành công!")
                .storeId(savedOrder.getStore().getStoreId())
                .orderType(savedOrder.getOrderType())
                .note(savedOrder.getNote())
                .surcharge(savedOrder.getSurcharge())
                .items(savedOrder.getOrderItems().stream().map(item ->
                        OrderResponse.OrderItemDto.builder()
                                .productId(item.getProduct().getProductId())
                                .productName(item.getProduct().getProductName())
                                .quantity(item.getQuantity())
                                .priceAtOrder(item.getPriceAtOrder())
                                .subTotal(item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }

    private String generateSmartOrderId(String prefix, String storeId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("%s-%s-%s-%s", prefix, storeId, datePart, randomPart);
    }

    // =========================================================================
    // CÁC HÀM XEM LỊCH SỬ, CHI TIẾT, HỦY, GOM ĐƠN (GIỮ NGUYÊN)
    // =========================================================================
    public List<OrderHistoryResponse> getOrderHistory(String storeId) {
        List<Order> orders = orderRepository.findByStore_StoreIdOrderByCreatedAtDesc(storeId);
        return orders.stream()
                .map(order -> OrderHistoryResponse.builder()
                        .orderId(order.getOrderId())
                        .orderType(order.getOrderType().name())
                        .status(order.getStatus().name())
                        .totalAmount(order.getTotalAmount())
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    public OrderDetailResponse getOrderDetail(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với mã: " + orderId));

        List<OrderDetailResponse.OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderDetailResponse.OrderItemDto.builder()
                        .productId(item.getProduct().getProductId())
                        .productName(item.getProduct().getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPriceAtOrder())
                        .lineTotal(item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .storeId(order.getStore().getStoreId())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .deliveryWindow(order.getDeliveryWindow() != null ? order.getDeliveryWindow().name() : null)
                .note(order.getNote())
                .totalAmount(order.getTotalAmount())
                .surcharge(order.getSurcharge())
                .createdAt(order.getCreatedAt())
                .items(itemDtos)
                .build();
    }

    @Transactional
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với mã: " + orderId));

        if (!order.getStatus().name().equals("NEW")) {
            throw new IllegalStateException("Không thể hủy! Đơn hàng đang ở trạng thái: " + order.getStatus().name());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public java.util.List<com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse> getPendingProductionAggregation() {
        java.util.List<Order> pendingOrders = orderRepository.findByStatus(Order.OrderStatus.NEW);
        java.util.Map<String, com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse> aggregationMap = new java.util.HashMap<>();

        for (Order order : pendingOrders) {
            for (com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem item : order.getOrderItems()) {
                String productId = item.getProduct().getProductId();
                if (aggregationMap.containsKey(productId)) {
                    aggregationMap.get(productId).setTotalQuantity(aggregationMap.get(productId).getTotalQuantity() + item.getQuantity());
                } else {
                    aggregationMap.put(productId, com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse.builder()
                            .productId(productId)
                            .productName(item.getProduct().getProductName())
                            .totalQuantity(item.getQuantity())
                            .build());
                }
            }
        }
        return new java.util.ArrayList<>(aggregationMap.values());
    }

    @Transactional
    public void confirmProductionAndAggregateOrders() {
        java.util.List<Order> pendingOrders = orderRepository.findByStatus(Order.OrderStatus.NEW);
        if (pendingOrders.isEmpty()) throw new RuntimeException("Không có đơn hàng mới nào để chốt nấu!");

        java.util.Map<String, Integer> productQuantities = new java.util.HashMap<>();
        for (Order order : pendingOrders) {
            for (com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem item : order.getOrderItems()) {
                productQuantities.put(item.getProduct().getProductId(), productQuantities.getOrDefault(item.getProduct().getProductId(), 0) + item.getQuantity());
            }
        }

        for (java.util.Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest request = new com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest();
            request.setProductId(entry.getKey());
            request.setQuantity(new java.math.BigDecimal(entry.getValue()));
            productionService.createProductionRun(request);
        }

        for (Order order : pendingOrders) order.setStatus(Order.OrderStatus.AGGREGATED);
        orderRepository.saveAll(pendingOrders);
    }
}