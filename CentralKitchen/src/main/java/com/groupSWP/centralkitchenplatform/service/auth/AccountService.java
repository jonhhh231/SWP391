package com.groupSWP.centralkitchenplatform.service.auth;


import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountResponse> getAccountsExcludingAdmin() {
        List<Account> accounts = accountRepository.findAllExcludingAdmin();
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // HÀM MỚI: Lấy danh sách theo trạng thái (Active / Inactive)
    public List<AccountResponse> getAccountsByStatus(boolean isActive) {
        List<Account> accounts = accountRepository.findByIsActiveExcludingAdmin(isActive);
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Hàm Helper để map Entity sang DTO (giúp code gọn gàng, không bị lặp)
    private AccountResponse mapToResponse(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setAccountId(account.getAccountId());
        dto.setUsername(account.getUsername());
        dto.setRole(account.getRole());
        dto.setActive(account.isActive()); // Trường này sẽ cho biết là true hay false

        SystemUser systemUser = account.getSystemUser();
        if (systemUser != null) {
            dto.setUserId(systemUser.getUserId());
            dto.setFullName(systemUser.getFullName());
            dto.setEmail(systemUser.getEmail());
        }
        return dto;
    }
}
