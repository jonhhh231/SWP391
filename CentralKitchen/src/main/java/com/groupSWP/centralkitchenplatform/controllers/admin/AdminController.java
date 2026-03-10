package com.groupSWP.centralkitchenplatform.controllers.admin;

import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.dto.auth.RegisterRequest;
import com.groupSWP.centralkitchenplatform.service.auth.AccountService;
import com.groupSWP.centralkitchenplatform.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các nghiệp vụ quản trị hệ thống dành riêng cho Admin.
 * <p>
 * Lớp này cung cấp các điểm cuối (endpoints) để Admin quản lý vòng đời của
 * tài khoản người dùng, bao gồm: cấp phát tài khoản mới, tra cứu và lọc danh sách nhân sự.
 * </p>
 * <p><b>Chính sách bảo mật:</b> Toàn bộ các API trong Controller này đều bị ràng buộc bởi
 * {@code @PreAuthorize("hasRole('ADMIN')")}. Bất kỳ Role nào khác cố tình gọi vào đây
 * đều sẽ nhận về lỗi 403 Forbidden.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final AccountService accountService;

    /**
     * API Cấp phát tài khoản nhân sự mới.
     * <p>
     * Phục vụ luồng nghiệp vụ: Admin là người duy nhất có quyền tạo tài khoản
     * cho nhân viên (Cửa hàng trưởng, Điều phối viên, Quản lý bếp...) và gắn họ vào
     * các cơ sở làm việc tương ứng.
     * </p>
     *
     * @param request Dữ liệu đầu vào chứa thông tin tài khoản (username, password, role, storeId...).
     * @return Thông báo xác nhận tạo tài khoản thành công cùng username vừa tạo.
     */
    @PostMapping("/register")
    public ResponseEntity<String> createEmployee(@RequestBody RegisterRequest request) {
        String result = authService.register(request);
        // Ở đây biến result đang chứa thông báo từ Service, bạn có thể cân nhắc trả thẳng result về,
        // hoặc giữ nguyên format của bạn nếu muốn đồng nhất message.
        return ResponseEntity.ok("Admin đã cấp tài khoản thành công! Username: " + request.username());
    }

    /**
     * API Tra cứu danh sách tài khoản linh hoạt (Tất cả hoặc theo từ khóa).
     * <p>
     * <b>Thiết kế tối ưu (2-in-1):</b>
     * <ul>
     * <li>Gửi Request không kèm tham số ({@code /list-accounts}): Lấy toàn bộ danh sách.</li>
     * <li>Gửi Request kèm tham số (VD: {@code /list-accounts?keyword=Nguyễn}): Trả về danh sách được lọc theo tên.</li>
     * </ul>
     * </p>
     *
     * @param keyword Từ khóa tìm kiếm (đối chiếu với thuộc tính FullName của SystemUser). Không bắt buộc.
     * @return Danh sách đối tượng {@link AccountResponse} đã được chuẩn hóa dữ liệu đầu ra.
     */
    @GetMapping("/list-accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam(required = false) String keyword) {

        // Nhờ logic xử lý thông minh đã viết bên AccountService,
        // Controller giờ đây chỉ cần gọi đúng 1 dòng này là xử lý được cả 2 trường hợp!
        return ResponseEntity.ok(accountService.searchAccountsByFullName(keyword));
    }

    /**
     * API Lọc danh sách các tài khoản đang hoạt động (Active).
     * <p>
     * Thường được Frontend sử dụng để hiển thị danh sách nhân sự khả dụng
     * cho các nghiệp vụ phân công công việc.
     * </p>
     *
     * @return Danh sách tài khoản có trạng thái {@code status = true}.
     */
    @GetMapping("/list-accounts/active")
    public ResponseEntity<List<AccountResponse>> getActiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(true));
    }

    /**
     * API Lọc danh sách các tài khoản đã bị khóa/ngưng hoạt động (Inactive).
     * <p>
     * Phục vụ mục đích kiểm toán (audit) hoặc xem xét mở khóa tài khoản của Admin.
     * </p>
     *
     * @return Danh sách tài khoản có trạng thái {@code status = false}.
     */
    @GetMapping("/list-accounts/inactive")
    public ResponseEntity<List<AccountResponse>> getInactiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(false));
    }
}