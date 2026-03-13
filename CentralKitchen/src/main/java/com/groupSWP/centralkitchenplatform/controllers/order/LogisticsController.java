package com.groupSWP.centralkitchenplatform.controllers.order;

import com.groupSWP.centralkitchenplatform.service.inventory.ShipmentService;
import com.groupSWP.centralkitchenplatform.service.order.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller quản lý luồng điều phối và theo dõi vận chuyển (Logistics Management).
 * <p>
 * Lớp này cung cấp các giao diện lập trình (API) chuyên biệt dành cho bộ phận Điều phối viên
 * (Coordinator) hoặc Quản lý (Manager). Các nghiệp vụ chính bao gồm:
 * <ul>
 * <li>Truy xuất danh sách đơn hàng đã nấu xong, sẵn sàng bốc xếp.</li>
 * <li>Theo dõi trạng thái các chuyến xe đang lưu thông trên đường.</li>
 * <li>Thực hiện ghép đơn, tạo chuyến xe và phân bổ tài xế thủ công.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Bảo mật:</b> Toàn bộ API trong Controller này yêu cầu Token hợp lệ và
 * quyền truy cập tối thiểu từ cấp độ {@code COORDINATOR} trở lên.
 * </p>
 */
@RestController
@RequestMapping("/api/logistics/orders")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;
    private final ShipmentService shipmentService;

    /**
     * API Lấy danh sách các đơn hàng đang chờ bốc xếp.
     * <p>
     * Truy xuất các đơn hàng có trạng thái {@code READY_TO_SHIP} và chưa được gán
     * vào bất kỳ chuyến xe (Shipment) nào. Dữ liệu này được Frontend sử dụng để
     * hiển thị danh sách cho Điều phối viên chọn và ghép chuyến.
     * </p>
     *
     * @return Phản hồi HTTP 200 chứa danh sách các đơn hàng (Mã đơn, Tên cửa hàng, Loại đơn, Trạng thái).
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/ready")
    public ResponseEntity<List<Map<String, Object>>> getReadyOrders() {
        return ResponseEntity.ok(logisticsService.getReadyOrders());
    }

    /**
     * API Lấy danh sách các chuyến xe đang hoạt động.
     * <p>
     * Phục vụ màn hình giám sát (Tracking) của Điều phối viên. Truy xuất các chuyến xe
     * có trạng thái {@code PENDING} (Chờ xuất phát) hoặc {@code SHIPPING} (Đang giao).
     * </p>
     *
     * @return Phản hồi HTTP 200 chứa danh sách chuyến xe đang chạy (Mã xe, Tên tài xế, Biển số, Trạng thái).
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveShipments() {
        return ResponseEntity.ok(logisticsService.getActiveShipments());
    }

    /**
     * API Xem chi tiết danh sách hàng hóa trên một chuyến xe cụ thể.
     * <p>
     * Giúp Điều phối viên kiểm tra xem chuyến xe này đang chở tổng cộng bao nhiêu món,
     * số lượng từng món là bao nhiêu để đối chiếu trước khi xe lăn bánh.
     * </p>
     *
     * @param shipmentId Mã định danh của chuyến xe (Shipment ID).
     * @return Phản hồi HTTP 200 chứa danh sách chi tiết các mặt hàng trên xe.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/{shipmentId}/details")
    public ResponseEntity<List<Map<String, Object>>> getShipmentDetails(@PathVariable String shipmentId) {
        return ResponseEntity.ok(logisticsService.getShipmentDetails(shipmentId));
    }

    // =========================================================================
    // 5. TẠO CHUYẾN XE BẰNG TAY (THAY THẾ CHO AI)
    // =========================================================================
    /**
     * API Tạo chuyến xe giao hàng thủ công (Manual Allocation).
     * <p>
     * Nhận một danh sách các mã đơn hàng (Order IDs) do Điều phối viên tick chọn trên giao diện.
     * Hệ thống sẽ tự động khởi tạo một chuyến xe mới (Shipment) và gán tất cả các đơn hàng này vào xe đó.
     * </p>
     *
     * @param payload Chuỗi JSON chứa danh sách các mã đơn hàng cần ghép.
     * Ví dụ: {@code {"orderIds": ["ORD-001", "ORD-002"]}}.
     * @return Phản hồi HTTP 200 thông báo tạo chuyến xe thành công kèm tin nhắn xác nhận.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @PostMapping("/manual-allocate")
    public ResponseEntity<Map<String, String>> allocateManualRoutes(@RequestBody Map<String, List<String>> payload) {
        List<String> orderIds = payload.get("orderIds");

        // 🔥 Tối ưu: Xóa try-catch. Gọi thẳng Service, lỗi ném ra sẽ được GlobalExceptionHandler bắt gọn
        String message = shipmentService.createManualShipment(orderIds);

        return ResponseEntity.ok(Map.of("status", "success", "message", message));
    }

    /**
     * API Lấy lịch sử các chuyến xe đã hoàn tất.
     * <p>
     * Phục vụ cho mục đích đối soát, thống kê và xem lại luồng công việc.
     * Lọc ra các chuyến xe có trạng thái {@code DELIVERED} (Đã giao) hoặc {@code RESOLVED} (Đã xử lý xong sự cố).
     * </p>
     *
     * @return Phản hồi HTTP 200 chứa danh sách lịch sử chuyến xe, sắp xếp theo thời gian mới nhất.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getCompletedShipments() {
        return ResponseEntity.ok(logisticsService.getCompletedShipments());
    }

    /**
     * API Lấy danh sách nhân sự có thể đảm nhiệm vai trò giao hàng.
     * <p>
     * Lọc ra danh sách các tài khoản có Role là {@code COORDINATOR} để hiển thị lên
     * Dropdown cho Điều phối viên chọn người lái xe (gán tài xế vào chuyến).
     * Định dạng UUID đã được xử lý chuẩn hóa ở tầng Service để Frontend dễ đọc.
     * </p>
     *
     * @return Phản hồi HTTP 200 chứa danh sách ID, Username và Role của các nhân sự khả dụng.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MANAGER', 'ROLE_MANAGER', 'COORDINATOR', 'ROLE_COORDINATOR')")
    @GetMapping("/coordinators-list")
    public ResponseEntity<List<Map<String, Object>>> getCoordinatorAccounts() {
        return ResponseEntity.ok(logisticsService.getCoordinatorAccounts());
    }
}