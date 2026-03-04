package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.logistic.ShipmentDetail;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentRepository;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentDetailRepository;
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

            if (detail.getMissingQuantity() > 0) {
                hasIssue = true;
            }
        }

        shipment.setStatus(hasIssue ? Shipment.ShipmentStatus.ISSUE_REPORTED : Shipment.ShipmentStatus.DELIVERED);
        shipmentRepository.save(shipment);

        return hasIssue ? "Đã ghi nhận sự cố thiếu hàng. Chờ điều phối xử lý!" : "Chuyến hàng hoàn tất trọn vẹn!";
    }

    @Transactional
    public String createReplacementShipment(String originalShipmentId) {
        Shipment originalShipment = shipmentRepository.findById(originalShipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng gốc!"));

        if (originalShipment.getStatus() != Shipment.ShipmentStatus.ISSUE_REPORTED) {
            throw new RuntimeException("Chuyến hàng này không có báo cáo thiếu/lỗi để bù!");
        }

        // Tạo chuyến hàng Bù
        Shipment replacementShipment = Shipment.builder()
                .shipmentId(originalShipmentId + "-REP-" + System.currentTimeMillis() % 1000)
                .shipmentType(Shipment.ShipmentType.REPLACEMENT)
                .status(Shipment.ShipmentStatus.PENDING)
                .coordinator(originalShipment.getCoordinator())
                .shipmentDetails(new ArrayList<>())
                .build();

        // Lọc món bị thiếu để nhét vào chuyến Bù
        for (ShipmentDetail oldDetail : originalShipment.getShipmentDetails()) {
            int missingQty = oldDetail.getMissingQuantity();
            if (missingQty > 0) {
                ShipmentDetail newDetail = ShipmentDetail.builder()
                        .shipment(replacementShipment)
                        .product(oldDetail.getProduct())
                        .productName(oldDetail.getProductName())
                        .expectedQuantity(missingQty) // Số lượng giao bù = số lượng bị thiếu
                        .receivedQuantity(0)
                        .issueNote("Giao bù cho chuyến: " + originalShipmentId)
                        .build();
                replacementShipment.getShipmentDetails().add(newDetail);
            }
        }

        if (replacementShipment.getShipmentDetails().isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm nào bị thiếu để tạo chuyến bù!");
        }

        originalShipment.setStatus(Shipment.ShipmentStatus.RESOLVED);
        originalShipment.setResolvedAt(LocalDateTime.now());

        shipmentRepository.save(originalShipment);
        shipmentRepository.save(replacementShipment);

        return "Đã lên chuyến giao BÙ thành công! Mã chuyến mới: " + replacementShipment.getShipmentId();
    }
}