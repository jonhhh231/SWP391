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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<String> createEmployee(@RequestBody RegisterRequest request) {
        String result = authService.register(request);
        return ResponseEntity.ok("Admin đã cấp tài khoản thành công! Username: " + request.username());
    }

    // =======================================================
    // ĐÃ NÂNG CẤP: GỘP LẤY DANH SÁCH & TÌM KIẾM VÀO 1 API
    // GET /api/admin/list-accounts?keyword=Nguyễn
    // =======================================================
    @GetMapping("/list-accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam(required = false) String keyword) {

        // Nhờ logic xử lý thông minh đã viết bên AccountService,
        // Controller giờ đây chỉ cần gọi đúng 1 dòng này là xử lý được cả 2 trường hợp!
        return ResponseEntity.ok(accountService.searchAccountsByFullName(keyword));
    }

    @GetMapping("/list-accounts/active")
    public ResponseEntity<List<AccountResponse>> getActiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(true));
    }

    @GetMapping("/list-accounts/inactive")
    public ResponseEntity<List<AccountResponse>> getInactiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(false));
    }
}