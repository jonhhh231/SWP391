package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
// import com.groupSWP.centralkitchenplatform.entities.logistic.Order; // Bỏ comment nếu bạn đã có class Order
import com.groupSWP.centralkitchenplatform.repositories.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    // Cần có OrderRepository ở đây để xử lý chi tiết Order, tạm thời tôi thu gọn logic

    // BƯỚC 1: Báo cáo thực nhận
    // LƯU Ý: ID truyền vào giờ là String
    @Transactional
    public String reportIssue(String shipmentId, ReportShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến giao hàng!"));

        boolean hasIssue = false;

        // Quét các Order bị báo lỗi trong chuyến xe
        for (ReportShipmentRequest.OrderReport report : request.getReportedOrders()) {
            if (report.isMissing()) {
                hasIssue = true;
                // TODO: Gọi OrderRepository để cập nhật trạng thái của Order bị lỗi này
            }
        }

        // Cập nhật trạng thái Shipment. Chú ý cách gọi Enum lồng nhau: Shipment.ShipmentStatus
        if (hasIssue) {
            shipment.setStatus(Shipment.ShipmentStatus.ISSUE_REPORTED);
        } else {
            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
        }

        shipmentRepository.save(shipment);
        return hasIssue ? "Đã ghi nhận có đơn hàng bị thiếu. Chờ điều phối xử lý!" : "Chuyến hàng hoàn tất trọn vẹn!";
    }

    // BƯỚC 2: Điều phối viên tạo đơn BÙ
    @Transactional
    public String createReplacementShipment(String originalShipmentId) {
        Shipment originalShipment = shipmentRepository.findById(originalShipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng gốc!"));

        if (originalShipment.getStatus() != Shipment.ShipmentStatus.ISSUE_REPORTED) {
            throw new RuntimeException("Chuyến hàng này không có báo cáo thiếu/lỗi!");
        }

        // Tạo Đơn hàng mới bằng Constructor thường (vì Entity của bạn không có @Builder)
        Shipment replacementShipment = new Shipment();

        // Tạo ID mới cho đơn bù (Ví dụ: ID gốc thêm chữ -REP)
        replacementShipment.setShipmentId(originalShipmentId + "-REP");

        replacementShipment.setShipmentType(Shipment.ShipmentType.REPLACEMENT);
        replacementShipment.setStatus(Shipment.ShipmentStatus.PENDING);
        replacementShipment.setCoordinator(originalShipment.getCoordinator());
        replacementShipment.setOrders(new ArrayList<>());

        // TODO: Viết logic lấy ra các Order bị thiếu từ đơn cũ nhét vào đơn mới ở đây

        // Cập nhật đơn gốc
        originalShipment.setStatus(Shipment.ShipmentStatus.RESOLVED);
        originalShipment.setResolvedAt(LocalDateTime.now());

        // Lưu Database
        shipmentRepository.save(originalShipment);
        shipmentRepository.save(replacementShipment);

        return "Đã lên chuyến giao BÙ thành công! Mã chuyến mới: " + replacementShipment.getShipmentId();
    }
}