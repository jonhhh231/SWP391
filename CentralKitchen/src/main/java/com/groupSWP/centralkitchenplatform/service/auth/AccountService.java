package com.groupSWP.centralkitchenplatform.service.auth;

import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

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

        // 💡 LƯU Ý Ở ĐÂY:
        // Nếu trong file AccountResponse.java, 'role' là kiểu Account.Role thì dùng:
        dto.setRole(account.getRole().name());
        // Nếu 'role' trong AccountResponse.java là kiểu String thì sửa thành:
        // dto.setRole(account.getRole().name());

        dto.setActive(account.isActive());

        SystemUser systemUser = account.getSystemUser();
        if (systemUser != null) {
            dto.setUserId(systemUser.getUserId());
            dto.setFullName(systemUser.getFullName());
            dto.setEmail(systemUser.getEmail());
        }
        return dto;
    }

    @Transactional
    public String toggleAccountStatus(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản trong hệ thống!"));

        // 🛡️ BẢO MẬT: Không cho phép Admin tự khóa chính mình hoặc khóa Admin khác
        if (account.getRole() == Account.Role.ADMIN) {
            throw new RuntimeException("Lỗi bảo mật: Không thể khóa/xóa mềm tài khoản cấp ADMIN!");
        }

        // Đảo ngược trạng thái hiện tại (Đang true thành false, đang false thành true)
        account.setActive(!account.isActive());
        accountRepository.save(account);

        return account.isActive() ?
                "Đã MỞ KHÓA tài khoản " + account.getUsername() + " thành công!" :
                "Đã KHÓA (Xóa mềm) tài khoản " + account.getUsername() + " thành công!";
    }
}