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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductionService productionService;

    @Transactional
    public OrderResponse createStandardOrder(OrderRequest request) {
        // 1. Xác thực Cửa hàng có tồn tại không?
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Cửa hàng với ID: " + request.getStoreId()));

        // 2. Khởi tạo Đơn hàng (Header)
        Order order = new Order();

        // Gọi hàm sinh mã tự động thông minh ở dưới cùng
        // LƯU Ý: Nếu Entity Store của Sếp dùng tên là getId() thì sửa lại nhé!
        String newOrderId = generateSmartOrderId(store.getStoreId());
        order.setOrderId(newOrderId);

        order.setStore(store);
        order.setStatus(Order.OrderStatus.NEW);
        order.setOrderType(Order.OrderType.STANDARD);
        order.setDeliveryWindow(Order.DeliveryWindow.valueOf(request.getDeliveryWindow().toUpperCase()));
        order.setNote(request.getNote());
        order.setSurcharge(BigDecimal.ZERO); // Mặc định đơn chuẩn không có phụ phí

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 3. Xử lý từng món hàng Cửa hàng đặt (Detail)
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            // Tìm sản phẩm
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Sản phẩm với ID: " + itemReq.getProductId()));

            // Khởi tạo Chi tiết Đơn hàng
            OrderItem orderItem = new OrderItem();

            // Set Khóa phức hợp
            OrderItemKey itemKey = new OrderItemKey(order.getOrderId(), product.getProductId());
            orderItem.setId(itemKey);

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());

            // Lấy giá hiện tại của Product chốt vào Order (Tránh đổi giá sau này)
            // LƯU Ý: Nếu Entity Product của Sếp không dùng getPrice() thì sửa thành tên tương ứng nhé!
            BigDecimal currentPrice = product.getSellingPrice();
            orderItem.setPriceAtOrder(currentPrice);

            // Cộng dồn vào tổng tiền: quantity * price
            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            orderItems.add(orderItem);
        }

        // 4. Lưu vào Database
        order.setTotalAmount(totalAmount);

        // Nhờ CascadeType.ALL bên Entity, khi save Order nó sẽ tự save luôn list OrderItem này
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 5. Đóng gói trả kết quả về cho Frontend
        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .status(savedOrder.getStatus().name())
                .totalAmount(savedOrder.getTotalAmount())
                .message("Tạo đơn hàng tiêu chuẩn thành công!")
                .build();
    }

    // =========================================================================
    // HÀM HỖ TRỢ (HELPER METHODS)
    // =========================================================================

    /**
     * Sinh mã đơn hàng thông minh: STD-[Mã Cửa Hàng]-[YYMMDD]-[4 Ký tự random]
     * Ví dụ: STD-ST001-260224-A8B2
     */
    private String generateSmartOrderId(String storeId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("STD-%s-%s-%s", storeId, datePart, randomPart);
    }

    @Transactional
    public OrderResponse createUrgentOrder(OrderRequest request) {
        // 1. Phí phạt khẩn cấp: 100.000 VNĐ
        BigDecimal URGENT_SURCHARGE = new BigDecimal("100000");

        // 2. Xác thực Cửa hàng
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Cửa hàng với ID: " + request.getStoreId()));

        Order order = new Order();

        // 3. Đổi Prefix thành URG (Urgent) thay vì STD
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setOrderId(String.format("URG-%s-%s-%s", store.getStoreId(), datePart, randomPart));

        order.setStore(store);
        order.setStatus(Order.OrderStatus.NEW);

        // 4. Đánh dấu đây là đơn KHẨN CẤP
        order.setOrderType(Order.OrderType.URGENT);
        order.setDeliveryWindow(Order.DeliveryWindow.valueOf(request.getDeliveryWindow().toUpperCase()));
        order.setNote(request.getNote());

        // 5. Cộng phụ phí vào đơn
        order.setSurcharge(URGENT_SURCHARGE);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Sản phẩm với ID: " + itemReq.getProductId()));

            OrderItem orderItem = new OrderItem();
            OrderItemKey itemKey = new OrderItemKey(order.getOrderId(), product.getProductId());
            orderItem.setId(itemKey);
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());

            BigDecimal currentPrice = product.getSellingPrice();
            orderItem.setPriceAtOrder(currentPrice);

            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            orderItems.add(orderItem);
        }

        // 6. Tổng tiền thanh toán = Tiền hàng + Phụ phí khẩn cấp
        order.setTotalAmount(totalAmount.add(URGENT_SURCHARGE));
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .status(savedOrder.getStatus().name())
                .totalAmount(savedOrder.getTotalAmount())
                .message("Tạo đơn hàng KHẨN CẤP thành công! Đã cộng thêm 100k phí giao gấp.")
                .build();
    }

    // =========================================================================
    // HÀM XEM LỊCH SỬ ĐƠN HÀNG
    // =========================================================================
    public List<OrderHistoryResponse> getOrderHistory(String storeId) {

        // 1. Nhờ bọn bốc vác (Repository) lôi hết đơn của Cửa hàng này ra, xếp mới nhất lên đầu
        List<Order> orders = orderRepository.findByStore_StoreIdOrderByCreatedAtDesc(storeId);

        // 2. Ép nó vào cái khuôn DTO để trả về cho Frontend (Dùng Java 8 Stream siêu mượt)
        return orders.stream()
                .map(order -> OrderHistoryResponse.builder()
                        .orderId(order.getOrderId())
                        .orderType(order.getOrderType().name())
                        .status(order.getStatus().name())
                        .totalAmount(order.getTotalAmount())
                        // Vì Order kế thừa BaseEntity nên Sếp lấy getCreatedAt() thoải mái nhé
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    // =========================================================================
    // HÀM XEM CHI TIẾT 1 ĐƠN HÀNG
    // =========================================================================
    public OrderDetailResponse getOrderDetail(String orderId) {

        // 1. Nhờ kho bốc đúng cái đơn hàng đó ra
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với mã: " + orderId));

        // 2. Chuyển đổi danh sách món ăn (Moi ruột)
        List<OrderDetailResponse.OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderDetailResponse.OrderItemDto.builder()
                        .productId(item.getProduct().getProductId())
                        .productName(item.getProduct().getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPriceAtOrder())
                        .lineTotal(item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // 3. Đúc tất cả vào cái khuôn to bự để trả về cho Frontend
        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .storeId(order.getStore().getStoreId()) // Lấy ID của cửa hàng đặt đơn
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

    // =========================================================================
    // HÀM HỦY ĐƠN HÀNG (CANCEL ORDER)
    // =========================================================================
    @Transactional
    public void cancelOrder(String orderId) {

        // 1. Lôi cổ cái đơn hàng đó ra
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với mã: " + orderId));

        // 2. Kiểm tra trạng thái: Đã sang PROCESSING (hoặc khác NEW) thì cấm hủy!
        if (!order.getStatus().name().equals("NEW")) {
            throw new IllegalStateException("Không thể hủy! Đơn hàng đang ở trạng thái: " + order.getStatus().name() + ". Bếp đã tiếp nhận hoặc đang giao!");
        }

        // 3. Cập nhật trạng thái thành CANCELLED (Hủy)
        // LƯU Ý: Chỗ này Sếp thay 'OrderStatus.CANCELLED' bằng đúng cái tên Enum mà Sếp đang dùng nhé (ví dụ CANCELED, DA_HUY...)
        order.setStatus(Order.OrderStatus.CANCELLED);

        // 4. Lưu lại vào DB
        orderRepository.save(order);
    }

    // =========================================================================
    // HÀM TỔNG HỢP ĐƠN HÀNG CHO BẾP TRUNG TÂM (AGGREGATION)
    // =========================================================================
    public java.util.List<com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse> getPendingProductionAggregation() {

        // 1. Lùa tất cả các đơn hàng đang chờ (NEW) ra sân
        java.util.List<com.groupSWP.centralkitchenplatform.entities.logistic.Order> pendingOrders =
                orderRepository.findByStatus(com.groupSWP.centralkitchenplatform.entities.logistic.Order.OrderStatus.NEW);

        // 2. Dùng Map để gom nhóm và cộng dồn số lượng theo ID Món Ăn
        java.util.Map<String, com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse> aggregationMap = new java.util.HashMap<>();

        for (com.groupSWP.centralkitchenplatform.entities.logistic.Order order : pendingOrders) {
            for (com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem item : order.getOrderItems()) {
                String productId = item.getProduct().getProductId();
                String productName = item.getProduct().getProductName();
                int quantity = item.getQuantity();

                if (aggregationMap.containsKey(productId)) {
                    // Nếu món này đã có trong danh sách -> Lôi ra cộng dồn số lượng
                    com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse existing = aggregationMap.get(productId);
                    existing.setTotalQuantity(existing.getTotalQuantity() + quantity);
                } else {
                    // Nếu là món mới thấy lần đầu -> Ghi vào danh sách
                    aggregationMap.put(productId, com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse.builder()
                            .productId(productId)
                            .productName(productName)
                            .totalQuantity(quantity)
                            .build());
                }
            }
        }

        // 3. Đóng gói danh sách trả về cho Bếp trưởng
        return new java.util.ArrayList<>(aggregationMap.values());
    }

    // =========================================================================
    // HÀM CHỐT NẤU (TỰ ĐỘNG GOM ĐƠN + GỌI HÀM TRỪ KHO CỦA ĐẠT + ĐỔI TRẠNG THÁI)
    // =========================================================================
    @Transactional
    public void confirmProductionAndAggregateOrders() {

        // 1. Lùa tất cả đơn NEW ra
        java.util.List<com.groupSWP.centralkitchenplatform.entities.logistic.Order> pendingOrders =
                orderRepository.findByStatus(com.groupSWP.centralkitchenplatform.entities.logistic.Order.OrderStatus.NEW);

        if (pendingOrders.isEmpty()) {
            throw new RuntimeException("Không có đơn hàng mới nào để chốt nấu!");
        }

        // 2. Gom nhóm số lượng y như hàm GET lúc nãy
        java.util.Map<String, Integer> productQuantities = new java.util.HashMap<>();
        for (com.groupSWP.centralkitchenplatform.entities.logistic.Order order : pendingOrders) {
            for (com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem item : order.getOrderItems()) {
                String productId = item.getProduct().getProductId();
                productQuantities.put(productId, productQuantities.getOrDefault(productId, 0) + item.getQuantity());
            }
        }

        // 3. Vứt từng nhóm món ăn vào "Máy xay thịt" (Hàm tạo ProductionRun của Đạt)
        for (java.util.Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest request = new com.groupSWP.centralkitchenplatform.dto.kitchen.ProductionRequest();
            request.setProductId(entry.getKey());
            // Ép kiểu sang BigDecimal vì hàm của Đạt yêu cầu thế
            request.setQuantity(new java.math.BigDecimal(entry.getValue()));

            // BÙM! Lệnh này sẽ chạy trừ kho và tạo Batch Code!
            productionService.createProductionRun(request);
        }

        // 4. Đổi trạng thái TẤT CẢ các đơn hàng sang AGGREGATED (Đã gom)
        for (com.groupSWP.centralkitchenplatform.entities.logistic.Order order : pendingOrders) {
            order.setStatus(com.groupSWP.centralkitchenplatform.entities.logistic.Order.OrderStatus.AGGREGATED);
        }

        // Lưu lại một lượt là xong phim!
        orderRepository.saveAll(pendingOrders);
    }
}