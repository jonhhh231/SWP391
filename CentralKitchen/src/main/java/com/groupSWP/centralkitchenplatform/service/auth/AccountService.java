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

    public List<AccountResponse> getAccountsByStatus(boolean isActive) {
        List<Account> accounts = accountRepository.findByIsActiveExcludingAdmin(isActive);
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AccountResponse> getFreeStoreManagers() {
        return accountRepository.findFreeStoreManagers().stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<AccountResponse> searchAccountsByFullName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAccountsExcludingAdmin();
        }
        List<Account> accounts = accountRepository.searchByFullNameExcludingAdmin(keyword.trim());
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setAccountId(account.getAccountId());
        dto.setUsername(account.getUsername());
        dto.setRole(account.getRole().name());
        dto.setActive(account.isActive());

        SystemUser systemUser = account.getSystemUser();
        if (systemUser != null) {
            dto.setUserId(systemUser.getUserId());
            dto.setFullName(systemUser.getFullName());
            dto.setEmail(systemUser.getEmail());
        }

        if (account.getStore() != null) {
            dto.setStoreName(account.getStore().getName());
        }

        return dto;
    }

    // =========================================================================
    // 🔥 CÁC HÀM XỬ LÝ NGHIỆP VỤ NHÂN SỰ
    // =========================================================================

    @Transactional
    public AccountResponse changeAccountRole(String accountId, String newRoleName) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        try {
            Account.Role newRole = Account.Role.valueOf(newRoleName.toUpperCase());
            account.setRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ! Vui lòng kiểm tra lại: " + newRoleName);
        }

        accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse assignStoreToAccount(String accountId, String storeId) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        if (storeId == null || storeId.trim().isEmpty()) {
            account.setStore(null);
        } else {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng với ID: " + storeId));
            account.setStore(store);
        }

        accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional
    public String toggleAccountStatus(UUID accountId, UUID replacementAccountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản trong hệ thống!"));

        if (account.getRole() == Account.Role.ADMIN) {
            throw new RuntimeException("Lỗi bảo mật: Không thể khóa tài khoản cấp ADMIN!");
        }

        String detailedMessage = ""; // 🌟 Biến lưu câu thông báo chi tiết

        if (account.isActive()) {
            Store managedStore = account.getStore();

            if (managedStore != null) {
                if (replacementAccountId == null) {
                    throw new RuntimeException("Nhân viên này đang quản lý cửa hàng '" + managedStore.getName() + "'. Vui lòng chọn một nhân viên khác để THẾ CHỖ trước khi khóa!");
                }

                Account replacementAccount = accountRepository.findById(replacementAccountId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên thế chỗ!"));

                if (!replacementAccount.isActive()) {
                    throw new RuntimeException("Nhân viên thế chỗ đang bị khóa (Inactive). Vui lòng chọn nhân viên đang hoạt động!");
                }

                if (replacementAccount.getStore() != null) {
                    throw new RuntimeException("Nhân viên thế chỗ hiện đang quản lý một cửa hàng khác. Vui lòng chọn người đang trống việc!");
                }

                // 4. THAO TÁC LUÂN CHUYỂN AN TOÀN TRÁNH LỖI DUPLICATE:
                // Bước A: Đá người cũ ra khỏi ghế trước
                account.setStore(null);
                managedStore.setAccount(null); // 🛠️ FIX LỖI: Báo cho Cửa hàng biết nó đang bị trống ghế
                accountRepository.saveAndFlush(account);

                // Bước B: Đôn người mới lên ngồi vào ghế đó
                replacementAccount.setStore(managedStore);
                managedStore.setAccount(replacementAccount); // 🛠️ FIX LỖI: Báo cho Cửa hàng biết nó có chủ mới
                accountRepository.save(replacementAccount);

                // 🌟 Báo cáo chi tiết khi có luân chuyển
                detailedMessage = "Đã KHÓA (Sa thải) quản lý cũ [" + account.getUsername() + "] (ID: " + account.getAccountId() + "). " +
                        "Đã bổ nhiệm thành công quản lý mới [" + replacementAccount.getUsername() + "] (ID: " + replacementAccount.getAccountId() + ") " +
                        "vào tiếp quản cửa hàng [" + managedStore.getName() + "].";
            } else {
                detailedMessage = "Đã KHÓA tài khoản [" + account.getUsername() + "] (ID: " + account.getAccountId() + ") thành công!";
            }
        } else {
            detailedMessage = "Đã MỞ KHÓA tài khoản [" + account.getUsername() + "] (ID: " + account.getAccountId() + ") thành công!";
        }

        account.setActive(!account.isActive());
        accountRepository.save(account);

        return detailedMessage;
    }

    // =========================================================================
    // 🌟 NGHIỆP VỤ HOÁN ĐỔI VỊ TRÍ QUẢN LÝ (SWAP MANAGERS)
    // =========================================================================
    @Transactional
    public String swapManagers(UUID accountId1, UUID accountId2) {
        if (accountId1.equals(accountId2)) {
            throw new RuntimeException("Lỗi: Không thể hoán đổi cùng một người!");
        }

        Account acc1 = accountRepository.findById(accountId1)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên thứ 1!"));
        Account acc2 = accountRepository.findById(accountId2)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên thứ 2!"));

        if (!acc1.isActive() || !acc2.isActive()) {
            throw new RuntimeException("Cả hai tài khoản phải đang hoạt động (Active) mới có thể hoán đổi!");
        }

        Store store1 = acc1.getStore();
        Store store2 = acc2.getStore();

        if (store1 == null || store2 == null) {
            throw new RuntimeException("Cả hai nhân viên đều phải đang quản lý cửa hàng thì mới có thể hoán đổi cho nhau!");
        }

        // 🛠️ BƯỚC 1: THÁO GHẾ CẢ 2 NGƯỜI RA TRƯỚC (Để tránh lỗi Unique Key)
        acc1.setStore(null);
        store1.setAccount(null);

        acc2.setStore(null);
        store2.setAccount(null);

        // Ép Hibernate nhả dữ liệu ra liền
        accountRepository.saveAndFlush(acc1);
        accountRepository.saveAndFlush(acc2);

        // 🛠️ BƯỚC 2: TRÁO ĐỔI HỘ KHẨU (Swap)
        acc1.setStore(store2);
        store2.setAccount(acc1);

        acc2.setStore(store1);
        store1.setAccount(acc2);

        // Lưu lại kết quả cuối cùng
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        // 🌟 Trả về thông báo chi tiết
        return "Đã HOÁN ĐỔI VỊ TRÍ thành công! " +
                "Quản lý [" + acc1.getUsername() + "] chuyển sang tiệm [" + store2.getName() + "]. " +
                "Quản lý [" + acc2.getUsername() + "] chuyển sang tiệm [" + store1.getName() + "].";
    }
}