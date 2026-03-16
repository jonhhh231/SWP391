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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // =========================================================================
    // 🔥 TỰ ĐỘNG CHỐT ĐƠN VÀ CỘNG KHO SAU 6 TIẾNG QUÁ HẠN (CRON JOB)
    // =========================================================================
    @Transactional
    @Scheduled(cron = "0 */30 * * * *")
    public void autoResolveOverdueShipments() {
        LocalDateTime sixHoursAgo = LocalDateTime.now().minusHours(6);
        List<Shipment> overdueShipments = shipmentRepository.findByStatusAndDeliveredAtBefore(
                Shipment.ShipmentStatus.DELIVERED, sixHoursAgo
        );

        if (overdueShipments.isEmpty()) return;

        int count = 0;
        for (Shipment shipment : overdueShipments) {
            try {
                // Nhận tự động với tư cách ADMIN để bypass
                reportIssue(shipment.getShipmentId(), "ADMIN", null);
                count++;
            } catch (Exception e) {
                log.error("Lỗi khi tự động chốt chuyến xe {}: {}", shipment.getShipmentId(), e.getMessage());
            }
        }
        log.info("Đã tự động chốt và cộng kho thành công cho {} chuyến xe quá hạn 6 tiếng.", count);
    }

    // =========================================================================
    // 🔥 BÁO CÁO CỘNG KHO - BẢO MẬT CHÍNH CHỦ
    // =========================================================================
    @Transactional
    public String reportIssue(String shipmentId, String requestingStoreId, ReportShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến giao hàng!"));

        if (shipment.getOrders() == null || shipment.getOrders().isEmpty()) {
            throw new RuntimeException("Chuyến xe rỗng, không thể xác định cửa hàng nhận!");
        }

        Store targetStore = shipment.getOrders().get(0).getStore();

        if (!"ADMIN".equals(requestingStoreId) && !targetStore.getStoreId().equals(requestingStoreId)) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền xem hoặc xác nhận cho chuyến xe của cửa hàng khác!");
        }

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

        // CỘNG DỒN KHO
        String storeId = targetStore.getStoreId();
        List<StockKey> stockKeys = shipment.getShipmentDetails().stream()
                .filter(d -> d.getReceivedQuantity() > 0)
                .map(d -> new StockKey(storeId, d.getProduct().getProductId()))
                .toList();

        Map<StockKey, Stock> existingStocksMap = stockRepository.findAllById(stockKeys)
                .stream()
                .collect(Collectors.toMap(Stock::getId, s -> s));

        List<Stock> stocksToSave = new ArrayList<>();

        for (ShipmentDetail detail : shipment.getShipmentDetails()) {
            if (detail.getReceivedQuantity() > 0) {
                StockKey key = new StockKey(storeId, detail.getProduct().getProductId());
                Stock stock = existingStocksMap.getOrDefault(key, new Stock());

                if (stock.getId() == null) {
                    stock.setId(key);
                    stock.setQuantity(0);
                    stock.setStore(targetStore);
                    stock.setProduct(detail.getProduct());
                }

                stock.setQuantity(stock.getQuantity() + detail.getReceivedQuantity());
                stocksToSave.add(stock);
            }
        }

        stockRepository.saveAll(stocksToSave);
        log.info("Đã cập nhật kho cho cửa hàng {} từ chuyến xe {}", storeId, shipmentId);

        shipment.setStatus(hasIssue ? Shipment.ShipmentStatus.ISSUE_REPORTED : Shipment.ShipmentStatus.RESOLVED);
        shipmentRepository.save(shipment);

        Order.OrderStatus finalOrderStatus = hasIssue ? Order.OrderStatus.PARTIAL_RECEIVED : Order.OrderStatus.DONE;
        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(finalOrderStatus));
            orderRepository.saveAll(shipment.getOrders());
        }

        return hasIssue ? "Đã ghi nhận sự cố thiếu hàng. Đã báo cho Bếp trung tâm lên đơn bù!" : "Xác nhận nhận đủ hàng. Kho cửa hàng đã được cập nhật!";
    }

    // =========================================================================
    // 🔥 TẠO ĐƠN ĐỀN BÙ
    // =========================================================================
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

    // =========================================================================
    // 🔥 GÁN TÀI XẾ
    // =========================================================================
    @Transactional
    public void assignDriverToShipment(String shipmentId, String accountId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        if (shipment.getStatus() != Shipment.ShipmentStatus.PENDING) {
            throw new RuntimeException("Lỗi: Chỉ có thể gán tài xế cho chuyến xe đang ở trạng thái PENDING (Chờ xuất phát)!");
        }

        Account driver = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản tài xế!"));

        shipment.setDriver(driver);
        shipment.setDriverName(driver.getUsername());
        shipment.setVehiclePlate(null);

        shipment.setStatus(Shipment.ShipmentStatus.SHIPPING);

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

    // =========================================================================
    // 🔥 TÀI XẾ BẤM "GIAO XONG" - BẢO MẬT CHÍNH CHỦ
    // =========================================================================
    @Transactional
    public void markShipmentAsDelivered(String shipmentId, String currentUsername) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        if (shipment.getStatus() != Shipment.ShipmentStatus.SHIPPING) {
            throw new RuntimeException("Chuyến xe này không ở trạng thái ĐANG GIAO (SHIPPING)!");
        }

        Account currentUser = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng!"));

        boolean isHighLevelManager = currentUser.getRole() == Account.Role.ADMIN || currentUser.getRole() == Account.Role.MANAGER;

        if (!isHighLevelManager) {
            if (shipment.getDriver() == null || !shipment.getDriver().getAccountId().equals(currentUser.getAccountId())) {
                throw new RuntimeException("Lỗi bảo mật: Bạn không phải là tài xế được phân công cho chuyến xe này!");
            }
        }

        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now()); // LƯU MỐC ĐẾM GIỜ CHỐT TỰ ĐỘNG

        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(Order.OrderStatus.DELIVERED));
            orderRepository.saveAll(shipment.getOrders());
        }

        shipmentRepository.save(shipment);
        log.info("Chuyến xe {} đã tới nơi an toàn!", shipmentId);
    }

    // =========================================================================
    // 🔥 1 ĐƠN HÀNG = 1 CHUYẾN ĐI (SHOPEE STYLE)
    // =========================================================================
    @Transactional
    public String createManualShipment(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 đơn hàng để điều phối!");
        }

        List<Order> orders = orderRepository.findAllById(orderIds);

        boolean allReady = orders.stream().allMatch(o ->
                o.getStatus() == Order.OrderStatus.READY_TO_SHIP && o.getShipment() == null);

        if (!allReady) {
            throw new RuntimeException("Có đơn hàng không hợp lệ (đã được gán tài xế hoặc chưa ở trạng thái READY_TO_SHIP)!");
        }

        int shipmentCount = 0;
        List<Order> ordersToUpdate = new ArrayList<>();

        for (Order o : orders) {
            String timeStamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")) + shipmentCount;

            Shipment individualShipment = Shipment.builder()
                    .shipmentId("SHP-" + timeStamp)
                    .shipmentType(Shipment.ShipmentType.MAIN_ROUTE)
                    .status(Shipment.ShipmentStatus.PENDING)
                    .shipmentDetails(new ArrayList<>())
                    .orders(new ArrayList<>())
                    .build();

            Shipment savedShipment = shipmentRepository.save(individualShipment);

            o.setShipment(savedShipment);
            ordersToUpdate.add(o);

            List<ShipmentDetail> detailsToSave = new ArrayList<>();
            if (o.getOrderItems() != null) {
                for (OrderItem item : o.getOrderItems()) {
                    ShipmentDetail detail = ShipmentDetail.builder()
                            .shipment(savedShipment)
                            .product(item.getProduct())
                            .productName(item.getProduct().getProductName())
                            .expectedQuantity(item.getQuantity())
                            .receivedQuantity(0)
                            .build();
                    detailsToSave.add(detail);
                }
            }
            shipmentDetailRepository.saveAll(detailsToSave);
            shipmentCount++;
        }

        orderRepository.saveAll(ordersToUpdate);
        return "Đã tách và tạo thành công " + shipmentCount + " chuyến giao hàng độc lập cho từng đơn!";
    }
}