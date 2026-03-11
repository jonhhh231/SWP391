package com.groupSWP.centralkitchenplatform.service.auth;

import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;

    public List<AccountResponse> getAccountsExcludingAdmin() {
        List<Account> accounts = accountRepository.findAllExcludingAdmin();
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // HÀM MỚI: Lấy danh sách theo trạng thái (Active / Inactive)
    public List<AccountResponse> getAccountsByStatus(boolean isActive) {
        List<Account> accounts = accountRepository.findByIsActiveExcludingAdmin(isActive);
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AccountResponse> searchAccountsByFullName(String keyword) {
        // Kiểm tra an toàn: Nếu keyword rỗng, trả về toàn bộ danh sách
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAccountsExcludingAdmin();
        }

        // Gọi Repository để tìm kiếm và map sang DTO
        List<Account> accounts = accountRepository.searchByFullNameExcludingAdmin(keyword.trim());
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Hàm Helper để map Entity sang DTO
    private AccountResponse mapToResponse(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setAccountId(account.getAccountId());
        dto.setUsername(account.getUsername());
        dto.setRole(account.getRole().name());
        dto.setActive(account.isActive());

        // Lấy thông tin user
        SystemUser systemUser = account.getSystemUser();
        if (systemUser != null) {
            dto.setUserId(systemUser.getUserId());
            dto.setFullName(systemUser.getFullName());
            dto.setEmail(systemUser.getEmail());
        }

        // =========================================================
        // 🌟 BỔ SUNG ĐOẠN NÀY ĐỂ LẤY THÔNG TIN CỬA HÀNG (STORE)
        // =========================================================
        if (account.getStore() != null) {
            // Nhờ dòng getStore() này, FetchType.LAZY mới chịu chạy query lấy data
            dto.setStoreName(account.getStore().getName());

            // Nếu AccountResponse của bạn có thêm thuộc tính storeName thì mở comment dòng dưới:
            // dto.setStoreName(account.getStore().getStoreName());
        }

        return dto;
    }

    @Transactional
    public AccountResponse changeAccountRole(String accountId, String newRoleName) {
        // 1. Tìm tài khoản (Lưu ý: accountId của bạn đang dùng UUID theo hệ thống)
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        // 2. Chuyển String thành Enum Role (Ví dụ: "STORE_MANAGER")
        try {
            Account.Role newRole = Account.Role.valueOf(newRoleName.toUpperCase());
            account.setRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ! Vui lòng kiểm tra lại: " + newRoleName);
        }

        // 3. Lưu xuống Database và trả về kết quả
        accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse assignStoreToAccount(String accountId, String storeId) {
        // 1. Tìm tài khoản
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        // 2. Xử lý logic gán cửa hàng
        if (storeId == null || storeId.trim().isEmpty()) {
            // Nếu không truyền storeId -> Xóa nhân viên khỏi cửa hàng hiện tại (Đưa về hội sở)
            account.setStore(null);
        } else {
            // Nếu có storeId -> Tìm cửa hàng và gán vào
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng với ID: " + storeId));
            account.setStore(store);
        }

        // 3. Lưu xuống DB
        accountRepository.save(account);
        return mapToResponse(account);
    }
}