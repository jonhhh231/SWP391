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

                account.setStore(null);
                accountRepository.saveAndFlush(account);

                replacementAccount.setStore(managedStore);
                accountRepository.save(replacementAccount);
            }
        }

        account.setActive(!account.isActive());
        accountRepository.save(account);

        return account.isActive() ?
                "Đã MỞ KHÓA tài khoản " + account.getUsername() :
                "Đã KHÓA tài khoản " + account.getUsername() + " thành công!";
    }
}