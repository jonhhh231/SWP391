package com.groupSWP.centralkitchenplatform.service;


import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
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

        return accounts.stream().map(account -> {
            AccountResponse dto = new AccountResponse();
            dto.setAccountId(account.getAccountId());
            dto.setUsername(account.getUsername());
            dto.setRole(account.getRole());
            dto.setActive(account.isActive());

            // Lấy thông tin chi tiết từ SystemUser nếu có
            SystemUser systemUser = account.getSystemUser();
            if (systemUser != null) {
                dto.setUserId(systemUser.getUserId());
                dto.setFullName(systemUser.getFullName());
                dto.setEmail(systemUser.getEmail());
            }

            return dto;
        }).collect(Collectors.toList());
    }
}
