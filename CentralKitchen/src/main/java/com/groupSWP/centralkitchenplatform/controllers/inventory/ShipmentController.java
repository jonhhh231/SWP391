package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.ReportShipmentRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Controller quản lý luồng vận hành giao nhận hàng hóa (Logistics & Shipment).
 * <p>
 * Phân hệ này chịu trách nhiệm điều phối toàn bộ vòng đời của một chuyến xe vận chuyển,
 * từ khâu gán tài xế, theo dõi lộ trình đến điểm đích, cho tới khi hoàn tất đối soát
 * và xử lý các kịch bản đền bù hàng hóa nếu có sai sót phát sinh.
 * </p>
 * <p>
 * <b>Các giai đoạn chính trong luồng (Workflows):</b>
 * <ul>
 * <li><b>Assignment:</b> Điều phối viên chỉ định phương tiện và nhân sự vận chuyển.</li>
 * <li><b>In Transit:</b> Giám sát trạng thái di chuyển của lô hàng trên đường.</li>
 * <li><b>Receiving & Audit:</b> Cửa hàng trưởng thực hiện kiểm đếm thực tế (Physical count).</li>
 * <li><b>Dispute Resolution:</b> Bếp trung tâm xử lý khiếu nại và giao bù (Replacement).</li>
 * </ul>
 * </p>
 *
 * @author Đạt, Huy, Triển
 * @version 1.0
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final AccountRepository accountRepository; // Bơm thêm repo để kiểm tra Role và Store

    /**
     * Phương thức hỗ trợ (Utility Method) để phân tách Store ID từ Security Principal.
     * <p>
     * Việc sử dụng hàm helper này giúp đảm bảo tính đóng gói (Encapsulation) và tái sử dụng
     * logic truy vấn thông tin cửa hàng từ Token bảo mật trong toàn bộ Controller.
     * Hỗ trợ cơ chế phân quyền dựa trên phạm vi cửa hàng (Store-based Authorization).
     * </p>
     *
     * @param principal Đối tượng chứa thông tin xác thực của người dùng hiện tại.
     * @return String chứa mã định danh cửa hàng hoặc rỗng nếu không thuộc cửa hàng nào.
     */
    // Hàm Helper lấy ID cửa hàng từ Token
    private String getStoreIdFromPrincipal(Principal principal) {
        if (principal == null) return null;
        Account account = accountRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        return account.getStore() != null ? account.getStore().getStoreId() : "";
    }

    /**
     * API Gán tài xế cho chuyến xe.
     * <p>
     * Thao tác này chuyển đổi trạng thái chuyến hàng từ chờ xử lý sang trạng thái chuẩn bị lăn bánh.
     * Hệ thống sẽ ghi nhận ID tài xế để phục vụ công tác liên lạc khi có sự cố.
     * </p>
     *
     * @param shipmentId Mã định danh duy nhất của chuyến xe cần gán.
     * @param payload Map chứa thông tin tài xế (key: accountId).
     * @return Phản hồi HTTP 200 kèm thông báo thành công.
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
     * <p>
     * Được gọi khi tài xế hoàn tất quãng đường di chuyển và có mặt tại điểm giao nhận.
     * Hệ thống ghi nhận thời điểm cập bến để tính toán SLA giao hàng.
     * </p>
     *
     * @param principal Principal lấy từ SecurityContext.
     * @param shipmentId ID của chuyến xe vừa đến đích.
     * @return Thông báo xác nhận xe tới nơi thành công.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COORDINATOR')") // Thêm Role tài xế vào đây nếu bạn có Role riêng cho họ nhé
    @PostMapping("/{shipmentId}/delivered")
    public ResponseEntity<?> markAsDelivered(Principal principal, @PathVariable String shipmentId) { // 🔥 Thêm Principal
        try {
            // Truyền username xuống để kiểm tra chính chủ
            shipmentService.markShipmentAsDelivered(shipmentId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Đã xác nhận xe tới nơi! Chờ Cửa hàng trưởng kiểm tra."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    /**
     * API Cửa hàng xem danh sách các chuyến xe ĐÃ ĐẾN NƠI và đang chờ mình xác nhận.
     * <p>API này tự động lấy storeId từ Token, cửa hàng nào chỉ thấy xe của cửa hàng đó.</p>
     *
     * @param principal Thông tin tài khoản Store Manager.
     * @return Danh sách các Shipment đang ở trạng thái DELIVERED.
     */
    @PreAuthorize("hasRole('STORE_MANAGER')")
    @GetMapping("/pending-report")
    public ResponseEntity<?> getShipmentsPendingReport(Principal principal) {
        try {
            // Lấy ID cửa hàng của người đang gọi API
            String storeId = getStoreIdFromPrincipal(principal);

            // Trả về danh sách
            return ResponseEntity.ok(shipmentService.getPendingReportShipmentsForStore(storeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API Cửa hàng trưởng chốt số lượng hàng thực nhận.
     * <p>
     * Cửa hàng trưởng thực hiện xác nhận số lượng khớp hoặc báo lỗi nếu có hàng hư hỏng/thiếu hụt.
     * Đây là bước cuối cùng trong luồng kiểm soát thất thoát (Loss Prevention).
     * </p>
     *
     * @param principal Principal xác thực.
     * @param shipmentId ID chuyến hàng cần chốt biên bản.
     * @param request Chi tiết danh sách hàng lỗi/thiếu.
     * @return Thông điệp kết quả xử lý báo cáo.
     */
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    @PostMapping("/{shipmentId}/report")
    public ResponseEntity<?> reportReceivedShipment(
            Principal principal, // 🔥 Thêm Principal
            @PathVariable String shipmentId,
            @RequestBody(required = false) ReportShipmentRequest request) {

        try {
            boolean isAdmin = false;
            if (principal != null) {
                Account account = accountRepository.findByUsername(principal.getName()).orElse(null);
                if (account != null && account.getRole() == Account.Role.ADMIN) {
                    isAdmin = true;
                }
            }

            // Gán chữ ADMIN hoặc lấy ID Cửa hàng thực tế
            String requestingStoreId = isAdmin ? "ADMIN" : getStoreIdFromPrincipal(principal);

            // Gọi service truyền đủ 3 tham số
            String result = shipmentService.reportIssue(shipmentId, requestingStoreId, request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API Bếp trung tâm xác nhận sự cố và lên đơn giao bù.
     * <p>
     * Khởi tạo một tiến trình vận chuyển đền bù dựa trên báo cáo sự cố từ cửa hàng.
     * </p>
     *
     * @param shipmentId ID chuyến xe gốc gặp sự cố.
     * @return Xác nhận đã tạo chuyến xe giao bù thành công.
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

    /**
     * API Lấy danh sách các chuyến xe bị Cửa hàng báo cáo thiếu hàng.
     * <p>Giúp Bếp trung tâm và Điều phối viên nắm được danh sách cần đền bù.</p>
     *
     * @return Danh sách các Shipment bị báo cáo sự cố (Reported issues).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN_MANAGER', 'COORDINATOR')")
    @GetMapping("/reported")
    public ResponseEntity<?> getReportedShipments() {
        try {
            return ResponseEntity.ok(shipmentService.getReportedShipments());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}