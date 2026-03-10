package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentDetailRepository shipmentDetailRepository;
    // BỔ SUNG: Gọi OrderRepository để xử lý trạng thái Đơn hàng
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public String reportIssue(String shipmentId, ReportShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến giao hàng!"));

        boolean hasIssue = false;

        // Cập nhật số lượng thực nhận từ báo cáo của Cửa hàng
        for (ReportShipmentRequest.ItemReport report : request.getReportedItems()) {
            ShipmentDetail detail = shipment.getShipmentDetails().stream()
                    .filter(d -> d.getProduct().getProductId().equals(report.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Sản phẩm " + report.getProductId() + " không có trong chuyến hàng này!"));

            detail.setReceivedQuantity(report.getReceivedQuantity());
            detail.setIssueNote(report.getNote());

            // Đảm bảo sếp đã viết hàm getMissingQuantity() trong class ShipmentDetail nhé
            if (detail.getMissingQuantity() > 0) {
                hasIssue = true;
            }
        }

        // [SỬA 1]: Cập nhật trạng thái CHUYẾN XE
        shipment.setStatus(hasIssue ? Shipment.ShipmentStatus.ISSUE_REPORTED : Shipment.ShipmentStatus.DELIVERED);
        shipmentRepository.save(shipment);

        // [SỬA 2]: Cập nhật trạng thái TOÀN BỘ ĐƠN HÀNG TRONG XE
        Order.OrderStatus finalOrderStatus = hasIssue ? Order.OrderStatus.PARTIAL_RECEIVED : Order.OrderStatus.DONE;
        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(finalOrderStatus));
            orderRepository.saveAll(shipment.getOrders());
        }

        return hasIssue ? "Đã ghi nhận sự cố thiếu hàng. Chờ điều phối xử lý!" : "Chuyến hàng hoàn tất trọn vẹn!";
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

    // 🔥 HÀM GÁN TÀI XẾ (Bị thiếu nãy giờ)
    @Transactional
    public void assignDriverToShipment(String shipmentId, String accountId, String vehiclePlate) {
        // 1. Tìm chuyến xe
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe: " + shipmentId));

        // 2. Tìm tài khoản COORDINATOR sẽ đi giao (Ép kiểu String sang UUID)
        Account driver = accountRepository.findById(java.util.UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản tài xế!"));

        // 3. Gán tài khoản vào xe
        shipment.setDriver( driver);

        // 4. Lấy Username làm tên tài xế luôn cho lẹ, bao lỗi 100%
        shipment.setDriverName(driver.getUsername());

        shipment.setVehiclePlate(vehiclePlate);

        // 5. Đổi trạng thái xe sang ĐANG ĐI GIAO
        shipment.setStatus(Shipment.ShipmentStatus.SHIPPING);

        // 6. Lưu xuống Database
        shipmentRepository.save(shipment);

        log.info("Đã gán thành công tài xế {} cho chuyến xe {}", driver.getUsername(), shipmentId);
    }

    // ... (code cũ của sếp giữ nguyên)

    // 🔥 [MỚI]: Hàm dành cho Tài xế bấm khi xe tới cửa hàng
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

        // 🔥 2. THÊM ĐOẠN NÀY: Đổi luôn trạng thái các Đơn hàng trên xe thành DELIVERED
        if (shipment.getOrders() != null) {
            shipment.getOrders().forEach(o -> o.setStatus(Order.OrderStatus.DELIVERED));
            orderRepository.saveAll(shipment.getOrders());
        }

        shipmentRepository.save(shipment);

        log.info("Chuyến xe {} đã tới nơi an toàn, chờ Cửa hàng trưởng kiểm tra!", shipmentId);
    }


} // <- Dấu ngoặc đóng class ShipmentService
