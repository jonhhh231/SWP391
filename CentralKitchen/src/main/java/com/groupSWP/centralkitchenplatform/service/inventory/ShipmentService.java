package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.logistic.ShipmentDetail;
import com.groupSWP.centralkitchenplatform.entities.product.Stock;
import com.groupSWP.centralkitchenplatform.entities.product.StockKey;

import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.StockRepository;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentRepository;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentDetailRepository;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentDetailRepository shipmentDetailRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;

    @Transactional
    public String reportIssue(String shipmentId, ReportShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến giao hàng!"));

        if (shipment.getStatus() != Shipment.ShipmentStatus.DELIVERED) {
            throw new RuntimeException("Chuyến xe chưa được đánh dấu là Đã Tới Nơi!");
        }

        boolean hasIssue = false;

        if (request != null && request.getReportedItems() != null && !request.getReportedItems().isEmpty()) {
            for (ReportShipmentRequest.ItemReport report : request.getReportedItems()) {
                ShipmentDetail detail = shipment.getShipmentDetails().stream()
                        .filter(d -> d.getProduct().getProductId().equals(report.getProductId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Sản phẩm " + report.getProductId() + " không có trong chuyến hàng này!"));

                detail.setReceivedQuantity(report.getReceivedQuantity());
                detail.setIssueNote(report.getNote());

                if (detail.getMissingQuantity() > 0) {
                    hasIssue = true;
                }
            }
        } else {
            shipment.getShipmentDetails().forEach(detail -> {
                detail.setReceivedQuantity(detail.getExpectedQuantity());
            });
        }

        // =====================================================================
        // 🔥 LOGIC MỚI: CỘNG DỒN VÀO KHO CỬA HÀNG (Lấy từ code cũ của bạn)
        // =====================================================================
        if (shipment.getOrders() != null && !shipment.getOrders().isEmpty()) {
            // Lấy thông tin cửa hàng (Vì 1 xe giao cho 1 cửa hàng)
            Store store = shipment.getOrders().get(0).getStore();
            String storeId = store.getStoreId();

            // 1. Tạo danh sách các Khóa (Key) dựa trên những món có thực nhận > 0
            List<StockKey> stockKeys = shipment.getShipmentDetails().stream()
                    .filter(d -> d.getReceivedQuantity() > 0)
                    .map(d -> new StockKey(storeId, d.getProduct().getProductId()))
                    .toList();

            // 2. Kéo dữ liệu Kho hiện tại lên RAM
            Map<StockKey, Stock> existingStocksMap = stockRepository.findAllById(stockKeys)
                    .stream()
                    .collect(Collectors.toMap(Stock::getId, s -> s));

            List<Stock> stocksToSave = new ArrayList<>();

            // 3. Xử lý cộng dồn số lượng dựa trên Số lượng thực nhận của xe
            for (ShipmentDetail detail : shipment.getShipmentDetails()) {
                if (detail.getReceivedQuantity() > 0) {
                    StockKey key = new StockKey(storeId, detail.getProduct().getProductId());
                    Stock stock = existingStocksMap.getOrDefault(key, new Stock());

                    // Nếu món này chưa từng có trong kho cửa hàng
                    if (stock.getId() == null) {
                        stock.setId(key);
                        stock.setQuantity(0); // Khởi tạo bằng 0
                        stock.setStore(store);
                        stock.setProduct(detail.getProduct());
                    }

                    // Cộng: Số lượng cũ + Số lượng thực nhận
                    stock.setQuantity(stock.getQuantity() + detail.getReceivedQuantity());
                    stocksToSave.add(stock);
                }
            }

            // 4. Lưu toàn bộ xuống DB
            stockRepository.saveAll(stocksToSave);
            log.info("Đã cập nhật kho cho cửa hàng {} từ chuyến xe {}", storeId, shipmentId);
        }
        // =====================================================================

        // Cập nhật trạng thái chuyến xe
        shipment.setStatus(hasIssue ? Shipment.ShipmentStatus.ISSUE_REPORTED : Shipment.ShipmentStatus.RESOLVED);
        shipmentRepository.save(shipment);

        // Cập nhật trạng thái đơn hàng
        Order.OrderStatus finalOrderStatus = hasIssue ? Order.OrderStatus.PARTIAL_RECEIVED : Order.OrderStatus.DONE;
        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(finalOrderStatus));
            orderRepository.saveAll(shipment.getOrders());
        }

        return hasIssue ? "Đã ghi nhận sự cố thiếu hàng. Đã báo cho Bếp trung tâm lên đơn bù!" : "Xác nhận nhận đủ hàng. Kho cửa hàng đã được cập nhật!";
    }

    @Transactional
    public String createReplacementShipment(String originalShipmentId) {
        Shipment originalShipment = shipmentRepository.findById(originalShipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng gốc!"));

        if (originalShipment.getStatus() != Shipment.ShipmentStatus.ISSUE_REPORTED) {
            throw new RuntimeException("Chuyến hàng này không có báo cáo thiếu/lỗi để bù!");
        }

        Shipment replacementShipment = Shipment.builder()
                .shipmentId(originalShipmentId + "-REP-" + System.currentTimeMillis() % 1000)
                .shipmentType(Shipment.ShipmentType.REPLACEMENT)
                .status(Shipment.ShipmentStatus.PENDING)
                .coordinator(originalShipment.getCoordinator())
                .shipmentDetails(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        Shipment savedReplacement = shipmentRepository.saveAndFlush(replacementShipment);

        Order compensationOrder = new Order();
        compensationOrder.setOrderId("COMP-" + System.currentTimeMillis() % 10000);
        compensationOrder.setOrderType(Order.OrderType.COMPENSATION);
        compensationOrder.setStatus(Order.OrderStatus.READY_TO_SHIP);
        compensationOrder.setShipment(savedReplacement);

        if (originalShipment.getOrders() != null && !originalShipment.getOrders().isEmpty()) {
            compensationOrder.setStore(originalShipment.getOrders().get(0).getStore());
        }

        for (ShipmentDetail oldDetail : originalShipment.getShipmentDetails()) {
            int missingQty = oldDetail.getMissingQuantity();
            if (missingQty > 0) {
                ShipmentDetail newDetail = ShipmentDetail.builder()
                        .shipment(savedReplacement)
                        .product(oldDetail.getProduct())
                        .productName(oldDetail.getProductName())
                        .expectedQuantity(missingQty)
                        .receivedQuantity(0)
                        .issueNote("Giao bù cho chuyến: " + originalShipmentId)
                        .build();
                savedReplacement.getShipmentDetails().add(newDetail);
            }
        }

        if (savedReplacement.getShipmentDetails().isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm nào bị thiếu để tạo chuyến bù!");
        }

        orderRepository.save(compensationOrder);
        shipmentDetailRepository.saveAll(savedReplacement.getShipmentDetails());

        originalShipment.setStatus(Shipment.ShipmentStatus.RESOLVED);
        originalShipment.setResolvedAt(LocalDateTime.now());
        shipmentRepository.save(originalShipment);

        return "Đã lên đơn BÙ (COMPENSATION) thành công! Mã chuyến mới: " + savedReplacement.getShipmentId();
    }

    @Transactional
    public void assignDriverToShipment(String shipmentId, String accountId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        // làm chốt không cho assign lại khi đã được giao thành công
        if (shipment.getStatus() != Shipment.ShipmentStatus.PENDING) {
            throw new RuntimeException("Lỗi: Chỉ có thể gán tài xế cho chuyến xe đang ở trạng thái PENDING (Chờ xuất phát)!");
        }

        Account driver = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản tài xế!"));

        shipment.setDriver(driver);
        shipment.setDriverName(driver.getUsername());
        shipment.setVehiclePlate(null);

        // Chuyển trạng thái xe sang đang giao
        shipment.setStatus(Shipment.ShipmentStatus.SHIPPING);

        // Chuyển đồng loạt các đơn hàng trên xe sang đang giao
        if (shipment.getOrders() != null && !shipment.getOrders().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            shipment.getOrders().forEach(order -> {
                order.setStatus(Order.OrderStatus.SHIPPING);
                order.setShippingStartTime(now);
            });
            orderRepository.saveAll(shipment.getOrders());
        }

        shipmentRepository.save(shipment);
        log.info("Đã gán tài xế {} cho chuyến xe {}.", driver.getUsername(), shipmentId);
    }

    @Transactional
    public void markShipmentAsDelivered(String shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        if (shipment.getStatus() != Shipment.ShipmentStatus.SHIPPING) {
            throw new RuntimeException("Chuyến xe này không ở trạng thái ĐANG GIAO (SHIPPING)!");
        }

        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);

        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(Order.OrderStatus.DELIVERED));
            orderRepository.saveAll(shipment.getOrders());
        }

        shipmentRepository.save(shipment);
        log.info("Chuyến xe {} đã tới nơi an toàn!", shipmentId);
    }

    @Transactional
    public String createManualShipment(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 đơn hàng để tạo chuyến xe!");
        }

        List<Order> orders = orderRepository.findAllById(orderIds);

        boolean allReady = orders.stream().allMatch(o ->
                o.getStatus() == Order.OrderStatus.READY_TO_SHIP && o.getShipment() == null);

        if (!allReady) {
            throw new RuntimeException("Có đơn hàng không hợp lệ (đã được gán xe hoặc chưa ở trạng thái READY_TO_SHIP)!");
        }

        String timeStamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));

        Shipment manualShipment = Shipment.builder()
                .shipmentId("MAN-" + timeStamp)
                .shipmentType(Shipment.ShipmentType.MAIN_ROUTE)
                .status(Shipment.ShipmentStatus.PENDING)
                .shipmentDetails(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        Shipment savedShipment = shipmentRepository.saveAndFlush(manualShipment);

        Map<String, ShipmentDetail> detailMap = new HashMap<>();

        for (Order o : orders) {
            o.setShipment(savedShipment);

            if (o.getOrderItems() != null) {
                for (OrderItem item : o.getOrderItems()) {
                    String productId = item.getProduct().getProductId();

                    ShipmentDetail detail = detailMap.getOrDefault(productId,
                            ShipmentDetail.builder()
                                    .shipment(savedShipment)
                                    .product(item.getProduct())
                                    .productName(item.getProduct().getProductName())
                                    .expectedQuantity(0)
                                    .receivedQuantity(0)
                                    .build());

                    detail.setExpectedQuantity(detail.getExpectedQuantity() + item.getQuantity());
                    detailMap.put(productId, detail);
                }
            }
        }

        orderRepository.saveAll(orders);
        shipmentDetailRepository.saveAll(detailMap.values());

        return "Đã tạo thành công chuyến xe: " + savedShipment.getShipmentId();
    }
}