package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller quản lý luồng vận hành giao nhận hàng hóa (Logistics & Shipment).
 * <p>
 * Lớp này xử lý vòng đời của một chuyến xe giao hàng, bao gồm:
 * Gán tài xế -> Xác nhận đến nơi -> Cửa hàng kiểm đếm -> Xử lý đền bù (nếu có).
 * </p>
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * API Gán tài xế cho chuyến xe.
     * <p>Được sử dụng bởi Điều phối viên hoặc Quản lý để bắt đầu tiến trình giao hàng.</p>
     *
     * @param shipmentId Mã chuyến xe cần gán tài xế.
     * @param payload    Bao gồm chuỗi accountId của tài xế.
     * @return Phản hồi HTTP 200 kèm thông báo thành công hoặc 400 nếu có lỗi nghiệp vụ.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @PostMapping("/{shipmentId}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable String shipmentId, @RequestBody Map<String, String> payload) {
        String accountId = payload.get("accountId");

        if (accountId == null || accountId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng truyền accountId của tài xế!"));
        }

        try {
            shipmentService.assignDriverToShipment(shipmentId, accountId);
            return ResponseEntity.ok(Map.of("message", "Gán tài xế thành công! Bắt đầu tính giờ giao hàng."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    /**
     * API Xác nhận tài xế đã đến cửa hàng.
     * <p>Chuyển trạng thái chuyến xe sang DELIVERED để Cửa hàng trưởng có thể tiến hành kiểm tra.</p>
     *
     * @param shipmentId Mã chuyến xe.
     * @return Phản hồi HTTP 200 kèm thông báo thành công hoặc 400 nếu có lỗi nghiệp vụ.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')")
    @PostMapping("/{shipmentId}/delivered")
    public ResponseEntity<?> markAsDelivered(@PathVariable String shipmentId) {
        try {
            shipmentService.markShipmentAsDelivered(shipmentId);
            return ResponseEntity.ok(Map.of("message", "Đã xác nhận xe tới nơi! Chờ Cửa hàng trưởng kiểm tra."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    /**
     * API Cửa hàng trưởng chốt số lượng hàng thực nhận.
     * <p>Nếu nhận đủ, đơn hàng hoàn tất. Nếu thiếu/hỏng, ghi nhận sự cố để xử lý đền bù.</p>
     *
     * @param shipmentId Mã chuyến xe.
     * @param request    Danh sách chi tiết các món bị báo cáo thiếu hoặc hỏng (nếu có).
     * @return Phản hồi HTTP 200 kèm thông báo kết quả kiểm hàng hoặc 400 nếu có lỗi.
     */
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    @PostMapping("/{shipmentId}/report")
    public ResponseEntity<?> reportReceivedShipment(
            @PathVariable String shipmentId,
            @RequestBody(required = false) ReportShipmentRequest request) {

        try {
            String result = shipmentService.reportIssue(shipmentId, request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API Bếp trung tâm xác nhận sự cố và lên đơn giao bù.
     * <p>Tạo một chuyến xe mới (REPLACEMENT) mang theo số lượng hàng bị thiếu để giao lại cho cửa hàng.</p>
     *
     * @param shipmentId Mã chuyến xe gốc bị thiếu hàng.
     * @return Phản hồi HTTP 200 thông báo mã chuyến bù mới được tạo hoặc 400 nếu có lỗi.
     */
    @PreAuthorize("hasAnyRole('KITCHEN_MANAGER', 'ADMIN')")
    @PostMapping("/{shipmentId}/resolve-replacement")
    public ResponseEntity<?> resolveAndCreateReplacement(@PathVariable String shipmentId) {
        try {
            String result = shipmentService.createReplacementShipment(shipmentId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}