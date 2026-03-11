package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.logistic.ShipmentDetail;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentDetailRepository shipmentDetailRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public String reportIssue(String shipmentId, ReportShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến giao hàng!"));

        // Nếu xe chưa tới nơi mà đòi kiểm hàng thì chặn lại
        if (shipment.getStatus() != Shipment.ShipmentStatus.DELIVERED) {
            throw new RuntimeException("Chuyến xe chưa được đánh dấu là Đã Tới Nơi!");
        }

        boolean hasIssue = false;

        // Nếu Store Manager có gửi danh sách báo cáo thiếu/lỗi
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
            // Nút "Đã nhận đủ hàng": Mặc định set số lượng thực nhận = số lượng mong đợi cho tất cả
            shipment.getShipmentDetails().forEach(detail -> {
                detail.setReceivedQuantity(detail.getExpectedQuantity());
            });
        }

        // 1. Cập nhật trạng thái CHUYẾN XE
        shipment.setStatus(hasIssue ? Shipment.ShipmentStatus.ISSUE_REPORTED : Shipment.ShipmentStatus.RESOLVED);
        shipmentRepository.save(shipment);

        // 2. Cập nhật trạng thái TOÀN BỘ ĐƠN HÀNG TRONG XE
        Order.OrderStatus finalOrderStatus = hasIssue ? Order.OrderStatus.PARTIAL_RECEIVED : Order.OrderStatus.DONE;
        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(finalOrderStatus));
            orderRepository.saveAll(shipment.getOrders());
        }

        return hasIssue ? "Đã ghi nhận sự cố thiếu hàng. Đã báo cho Bếp trung tâm lên đơn bù!" : "Xác nhận nhận đủ hàng. Đơn hàng hoàn tất!";
    }

    @Transactional
    public String createReplacementShipment(String originalShipmentId) {
        Shipment originalShipment = shipmentRepository.findById(originalShipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng gốc!"));

        if (originalShipment.getStatus() != Shipment.ShipmentStatus.ISSUE_REPORTED) {
            throw new RuntimeException("Chuyến hàng này không có báo cáo thiếu/lỗi để bù!");
        }

        // 1. Tạo chuyến hàng Bù (REPLACEMENT)
        Shipment replacementShipment = Shipment.builder()
                .shipmentId(originalShipmentId + "-REP-" + System.currentTimeMillis() % 1000)
                .shipmentType(Shipment.ShipmentType.REPLACEMENT)
                .status(Shipment.ShipmentStatus.PENDING) // Chờ điều phối lại tài xế
                .coordinator(originalShipment.getCoordinator())
                .shipmentDetails(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        Shipment savedReplacement = shipmentRepository.saveAndFlush(replacementShipment);

        // 2. TẠO ĐƠN HÀNG ĐỀN BÙ (COMPENSATION ORDER)
        Order compensationOrder = new Order();
        compensationOrder.setOrderId("COMP-" + System.currentTimeMillis() % 10000);
        compensationOrder.setOrderType(Order.OrderType.COMPENSATION);
        compensationOrder.setStatus(Order.OrderStatus.READY_TO_SHIP); // Để sẵn sàng giao
        compensationOrder.setShipment(savedReplacement);

        // Lấy lại cửa hàng từ đơn cũ (Do 1 xe chở cho 1 cửa hàng)
        if (originalShipment.getOrders() != null && !originalShipment.getOrders().isEmpty()) {
            compensationOrder.setStore(originalShipment.getOrders().get(0).getStore());
        }

        // 3. Lọc món bị thiếu nhét vào Chi tiết xe
        for (ShipmentDetail oldDetail : originalShipment.getShipmentDetails()) {
            int missingQty = oldDetail.getMissingQuantity();
            if (missingQty > 0) {
                ShipmentDetail newDetail = ShipmentDetail.builder()
                        .shipment(savedReplacement)
                        .product(oldDetail.getProduct())
                        .productName(oldDetail.getProductName())
                        .expectedQuantity(missingQty) // Số lượng giao bù = số lượng bị thiếu
                        .receivedQuantity(0)
                        .issueNote("Giao bù cho chuyến: " + originalShipmentId)
                        .build();
                savedReplacement.getShipmentDetails().add(newDetail);
            }
        }

        if (savedReplacement.getShipmentDetails().isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm nào bị thiếu để tạo chuyến bù!");
        }

        // 4. Lưu Đơn hàng đền bù và Chi tiết xe
        orderRepository.save(compensationOrder);
        shipmentDetailRepository.saveAll(savedReplacement.getShipmentDetails());

        // 5. Khép lại hồ sơ chuyến cũ
        originalShipment.setStatus(Shipment.ShipmentStatus.RESOLVED);
        originalShipment.setResolvedAt(LocalDateTime.now());
        shipmentRepository.save(originalShipment);

        return "Đã lên đơn BÙ (COMPENSATION) thành công! Mã chuyến mới: " + savedReplacement.getShipmentId();
    }

    // 🔥 HÀM GÁN TÀI XẾ
    @Transactional
    public void assignDriverToShipment(String shipmentId, String accountId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        Account driver = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản tài xế!"));

        shipment.setDriver(driver);
        shipment.setDriverName(driver.getUsername());
        shipment.setVehiclePlate(null);

        // Đổi trạng thái xe sang ĐANG ĐI GIAO
        shipment.setStatus(Shipment.ShipmentStatus.SHIPPING);

        // KÍCH HOẠT BỘ ĐẾM 6 TIẾNG
        if (shipment.getOrders() != null && !shipment.getOrders().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            shipment.getOrders().forEach(order -> {
                order.setStatus(Order.OrderStatus.SHIPPING);
                order.setShippingStartTime(now); // Chốt giờ xuất phát!
            });
            orderRepository.saveAll(shipment.getOrders());
        }

        shipmentRepository.save(shipment);
        log.info("Đã gán tài xế {} cho chuyến xe {}. Bắt đầu đếm ngược 6 tiếng cho các đơn hàng!", driver.getUsername(), shipmentId);
    }

    // 🔥 Hàm dành cho Tài xế bấm khi xe tới cửa hàng
    @Transactional
    public void markShipmentAsDelivered(String shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        if (shipment.getStatus() != Shipment.ShipmentStatus.SHIPPING) {
            throw new RuntimeException("Chuyến xe này không ở trạng thái ĐANG GIAO (SHIPPING)!");
        }

        // 1. Đổi trạng thái xe thành ĐÃ TỚI NƠI
        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);

        // 2. Đổi luôn trạng thái các Đơn hàng trên xe thành DELIVERED
        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(Order.OrderStatus.DELIVERED));
            orderRepository.saveAll(shipment.getOrders());
        }

        shipmentRepository.save(shipment);
        log.info("Chuyến xe {} đã tới nơi an toàn, chờ Cửa hàng trưởng kiểm tra!", shipmentId);
    }

    // =========================================================================
    // 🔥 TẠO CHUYẾN XE BẰNG TAY (MANUAL ALLOCATION)
    // =========================================================================
    @Transactional
    public String createManualShipment(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 đơn hàng để tạo chuyến xe!");
        }

        // 1. Tìm các đơn hàng dựa trên danh sách ID được truyền vào
        List<Order> orders = orderRepository.findAllById(orderIds);

        // Kiểm tra an toàn: Xem có đơn nào chưa sẵn sàng hoặc đã bị gán xe khác rồi không
        boolean allReady = orders.stream().allMatch(o ->
                o.getStatus() == Order.OrderStatus.READY_TO_SHIP && o.getShipment() == null);

        if (!allReady) {
            throw new RuntimeException("Có đơn hàng không hợp lệ (đã được gán xe hoặc chưa ở trạng thái READY_TO_SHIP)!");
        }

        // 2. Tạo một chuyến xe mới cứng (Trạng thái PENDING - chờ gán tài xế)
        Shipment manualShipment = Shipment.builder()
                .shipmentId("MAN-" + System.currentTimeMillis() % 10000)
                .shipmentType(Shipment.ShipmentType.MAIN_ROUTE) // Chuyến gom đơn thường là tuyến chính
                .status(Shipment.ShipmentStatus.PENDING)
                .shipmentDetails(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        // Lưu chuyến xe xuống DB trước để lấy ID
        Shipment savedShipment = shipmentRepository.saveAndFlush(manualShipment);

        // 3. Quét qua các đơn hàng, gán xe vào đơn và cộng dồn số lượng từng món ăn
        Map<String, ShipmentDetail> detailMap = new HashMap<>();

        for (Order o : orders) {
            o.setShipment(savedShipment); // Gắn ID chuyến xe vào đơn hàng

            if (o.getOrderItems() != null) {
                for (OrderItem item : o.getOrderItems()) {
                    String productId = item.getProduct().getProductId();

                    // Nếu món này đã có trong xe (do đơn trước cộng vào) thì lấy ra, chưa có thì tạo mới
                    ShipmentDetail detail = detailMap.getOrDefault(productId,
                            ShipmentDetail.builder()
                                    .shipment(savedShipment)
                                    .product(item.getProduct())
                                    .productName(item.getProduct().getProductName())
                                    .expectedQuantity(0)
                                    .receivedQuantity(0)
                                    .build());

                    // Cộng dồn số lượng
                    detail.setExpectedQuantity(detail.getExpectedQuantity() + item.getQuantity());
                    detailMap.put(productId, detail);
                }
            }
        }

        // 4. Lưu lại toàn bộ cục thay đổi (Đơn hàng & Chi tiết xe) xuống Database
        orderRepository.saveAll(orders);
        shipmentDetailRepository.saveAll(detailMap.values());

        return "Đã tạo thành công chuyến xe: " + savedShipment.getShipmentId();
    }
}