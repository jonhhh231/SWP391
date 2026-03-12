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
import java.util.UUID;

/**
 * Controller xử lý các nghiệp vụ quản trị hệ thống dành riêng cho Admin.
 * <p>
 * Lớp này cung cấp các điểm cuối (endpoints) để Admin quản lý vòng đời của
 * tài khoản người dùng, bao gồm: cấp phát tài khoản mới, tra cứu và lọc danh sách nhân sự.
 * </p>
 * <p><b>Chính sách bảo mật:</b> Toàn bộ các API trong Controller này đều bị ràng buộc bởi quyền ADMIN.</p>
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
     */
    @PostMapping("/register")
    public ResponseEntity<String> createEmployee(@RequestBody RegisterRequest request) {
        String result = authService.register(request);
        return ResponseEntity.ok("Admin đã cấp tài khoản thành công! Username: " + request.username());
    }

    /**
     * API Tra cứu danh sách tài khoản linh hoạt (Tất cả hoặc theo từ khóa).
     */
    @GetMapping("/list-accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(accountService.searchAccountsByFullName(keyword));
    }

    /**
     * API Lọc danh sách các tài khoản đang hoạt động (Active).
     */
    @GetMapping("/list-accounts/active")
    public ResponseEntity<List<AccountResponse>> getActiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(true));
    }

    /**
     * API Lọc danh sách các tài khoản đã bị khóa/ngưng hoạt động (Inactive).
     */
    @GetMapping("/list-accounts/inactive")
    public ResponseEntity<List<AccountResponse>> getInactiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(false));
    }

    // =========================================================================
    // 🔥 CÁC NGHIỆP VỤ NHÂN SỰ MỚI (THĂNG CHỨC & CHUYỂN CÔNG TÁC)
    // =========================================================================

    /**
     * API Thay đổi chức vụ (Role) của tài khoản.
     * <p>Phục vụ nghiệp vụ thăng chức hoặc giáng chức nhân viên.</p>
     * <p>Đã áp dụng luật nghiệp vụ chặt chẽ: Thăng chức bắt buộc chọn Cửa hàng,
     * Giáng chức bắt buộc chọn Người thế chỗ.</p>
     *
     * @param accountId ID của tài khoản cần thay đổi (UUID).
     * @param roleName  Tên Role mới (VD: STORE_MANAGER, COORDINATOR...).
     * @return Thông báo chi tiết sau khi cập nhật Role và luân chuyển cửa hàng.
     */
    @PatchMapping("/accounts/{accountId}/role")
    public ResponseEntity<String> changeAccountRole( // 🌟 Đã sửa thành String
                                                     @PathVariable String accountId,
                                                     @RequestParam String roleName,
                                                     @RequestParam(required = false) String storeId,
                                                     @RequestParam(required = false) UUID replacementAccountId) {

        String message = accountService.changeAccountRole(accountId, roleName, storeId, replacementAccountId);
        return ResponseEntity.ok(message); // 🌟 Trả về String
    }

    /**
     * API Khóa / Mở khóa tài khoản (Xóa mềm) kèm luân chuyển nhân sự.
     */
    @PutMapping("/accounts/{accountId}/status")
    public ResponseEntity<String> toggleAccountStatus(
            @PathVariable UUID accountId,
            @RequestParam(required = false) UUID replacementAccountId) {
        return ResponseEntity.ok(accountService.toggleAccountStatus(accountId, replacementAccountId));
    }

    /**
     * API Lấy danh sách các Quản lý cửa hàng đang "Trống việc" (Dự bị).
     */
    @GetMapping("/list-accounts/free-managers")
    public ResponseEntity<List<AccountResponse>> getFreeManagers() {
        return ResponseEntity.ok(accountService.getFreeStoreManagers());
    }

    /**
     * API Gán hoặc thay đổi cửa hàng làm việc cho tài khoản.
     * <p>Sử dụng để gán cửa hàng mới hoặc rút nhân sự về dự bị (storeId = null).</p>
     */
    @PatchMapping("/accounts/{accountId}/store")
    public ResponseEntity<String> assignStoreToAccount( // 🌟 Đã sửa thành String
                                                        @PathVariable String accountId,
                                                        @RequestParam(required = false) String storeId) {

        String message = accountService.assignStoreToAccount(accountId, storeId);
        return ResponseEntity.ok(message); // 🌟 Trả về String
    }

    /**
     * API Hoán đổi vị trí cửa hàng giữa 2 Quản lý.
     * <p>
     * Phục vụ nghiệp vụ luân chuyển chéo: Ông A sang tiệm B, Ông B sang tiệm A.
     * </p>
     *
     * @param accountId1 ID của quản lý thứ 1
     * @param accountId2 ID của quản lý thứ 2
     * @return Thông báo kết quả hoán đổi chi tiết
     */
    @PutMapping("/accounts/swap-stores")
    public ResponseEntity<String> swapStoreManagers(
            @RequestParam UUID accountId1,
            @RequestParam UUID accountId2) {

        return ResponseEntity.ok(accountService.swapManagers(accountId1, accountId2));
    }
}