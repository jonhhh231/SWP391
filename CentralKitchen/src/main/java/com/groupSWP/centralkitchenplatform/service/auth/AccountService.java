package com.groupSWP.centralkitchenplatform.service.auth;

import com.groupSWP.centralkitchenplatform.dto.auth.AccountResponse;
import com.groupSWP.centralkitchenplatform.dto.auth.UpdateAccountRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.auth.SystemUserRepository;
import com.groupSWP.centralkitchenplatform.repositories.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final SystemUserRepository systemUserRepository; // 🌟 Thêm cái này để lấy user
    private final PasswordEncoder passwordEncoder; // 🌟 Thêm cái này để mã hóa mật khẩu mới

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

    // ======================================================
    // 🛠️ ĐỔI ROLE (Có thông báo chi tiết)
    // ======================================================
    @Transactional
    public String changeAccountRole(String accountId, String newRoleName, String storeId, UUID replacementAccountId) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        Account.Role oldRole = account.getRole();
        Account.Role newRole;
        try {
            newRole = Account.Role.valueOf(newRoleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ! Vui lòng kiểm tra lại: " + newRoleName);
        }

        if (oldRole == newRole) {
            return "Chức vụ không thay đổi, tài khoản [" + account.getUsername() + "] vẫn là " + oldRole;
        }

        String thongBao = "";

        // 🛑 LUẬT 1: LÊN LÀM QUẢN LÝ (Promote)
        if (newRole == Account.Role.STORE_MANAGER) {
            if (storeId == null || storeId.trim().isEmpty()) {
                throw new RuntimeException("Nghiệp vụ bắt buộc: Khi thăng chức lên Cửa hàng trưởng, BẮT BUỘC phải chọn một Cửa hàng (storeId) để bổ nhiệm!");
            }
            Store newStore = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng với ID: " + storeId));

            if (newStore.getAccount() != null) {
                throw new RuntimeException("Cửa hàng này ĐÃ CÓ người quản lý! Vui lòng chọn cửa hàng đang trống.");
            }

            account.setStore(newStore);
            newStore.setAccount(account);
            thongBao = "Đã THĂNG CHỨC tài khoản [" + account.getUsername() + "] lên làm Cửa hàng trưởng và giao tiếp quản tiệm [" + newStore.getName() + "].";
        }
        // 🛑 LUẬT 2: GIÁNG CHỨC / ĐỔI NGÀNH (Demote)
        else if (oldRole == Account.Role.STORE_MANAGER) {
            Store managedStore = account.getStore();

            if (managedStore != null) {
                if (replacementAccountId == null) {
                    throw new RuntimeException("Tài khoản này đang quản lý cửa hàng [" + managedStore.getName() + "]. BẮT BUỘC phải chọn người thế chỗ trước khi chuyển chức vụ!");
                }

                Account replacementAccount = accountRepository.findById(replacementAccountId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân sự thế chỗ!"));

                if (!replacementAccount.isActive() || replacementAccount.getRole() != Account.Role.STORE_MANAGER) {
                    throw new RuntimeException("Người thế chỗ phải đang Hoạt động (Active) và phải mang chức vụ Quản lý (STORE_MANAGER)!");
                }
                if (replacementAccount.getStore() != null) {
                    throw new RuntimeException("Người thế chỗ hiện đang quản lý một cửa hàng khác. Vui lòng chọn người đang trống việc!");
                }

                // Luân chuyển an toàn
                account.setStore(null);
                managedStore.setAccount(null);
                accountRepository.saveAndFlush(account);

                replacementAccount.setStore(managedStore);
                managedStore.setAccount(replacementAccount);
                accountRepository.save(replacementAccount);

                thongBao = "Đã CHUYỂN CÔNG TÁC tài khoản [" + account.getUsername() + "] sang bộ phận [" + newRole + "]. " +
                        "Đồng thời bàn giao thành công tiệm [" + managedStore.getName() + "] cho quản lý mới [" + replacementAccount.getUsername() + "].";
            } else {
                thongBao = "Đã đổi chức vụ của quân dự bị [" + account.getUsername() + "] từ Quản lý sang [" + newRole + "].";
            }
        }
        // 🛑 LUẬT 3: ĐỔI ROLE BÌNH THƯỜNG KHÁC
        else {
            thongBao = "Đã đổi chức vụ của [" + account.getUsername() + "] từ [" + oldRole + "] sang [" + newRole + "].";
        }

        account.setRole(newRole);
        accountRepository.save(account);

        return thongBao;
    }

    // ======================================================
    // 🛠️ GÁN/THÁO CỬA HÀNG (Có thông báo chi tiết)
    // ======================================================
    @Transactional
    public String assignStoreToAccount(String accountId, String storeId) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        if (account.getRole() != Account.Role.STORE_MANAGER) {
            throw new RuntimeException("Chỉ tài khoản STORE_MANAGER mới có thể gán hoặc tháo cửa hàng!");
        }

        Store oldStore = account.getStore();
        String thongBao = "";

        // NẾU THÁO CỬA HÀNG (Rút về dự bị)
        if (storeId == null || storeId.trim().isEmpty()) {
            if (oldStore != null) {
                oldStore.setAccount(null);
                account.setStore(null);
                thongBao = "Đã RÚT QUẢN LÝ [" + account.getUsername() + "] khỏi tiệm [" + oldStore.getName() + "]. Nhân sự này hiện đang chờ phân công mới (Quân dự bị).";
            } else {
                thongBao = "Tài khoản [" + account.getUsername() + "] hiện tại đã là Quân dự bị rồi, không có cửa hàng nào để rút!";
            }
        }
        // NẾU GÁN VÀO CỬA HÀNG MỚI
        else {
            Store newStore = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng với ID: " + storeId));

            if (newStore.getAccount() != null && !newStore.getAccount().getAccountId().equals(account.getAccountId())) {
                throw new RuntimeException("Cửa hàng đích ĐÃ CÓ người quản lý rồi! Không thể gán thêm.");
            }

            if (oldStore != null && !oldStore.getStoreId().equals(storeId)) {
                oldStore.setAccount(null);
                account.setStore(null);
                accountRepository.saveAndFlush(account);
                thongBao = "Đã LUÂN CHUYỂN quản lý [" + account.getUsername() + "] từ tiệm [" + oldStore.getName() + "] sang tiếp quản tiệm MỚI [" + newStore.getName() + "].";
            } else if (oldStore == null) {
                thongBao = "Đã BỔ NHIỆM quản lý dự bị [" + account.getUsername() + "] vào tiếp quản tiệm [" + newStore.getName() + "].";
            } else {
                thongBao = "Nhân sự [" + account.getUsername() + "] đang quản lý tiệm [" + newStore.getName() + "] này rồi!";
            }

            account.setStore(newStore);
            newStore.setAccount(account);
        }

        accountRepository.save(account);
        return thongBao;
    }

    @Transactional
    public String toggleAccountStatus(UUID accountId, UUID replacementAccountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản trong hệ thống!"));

        if (account.getRole() == Account.Role.ADMIN) {
            throw new RuntimeException("Lỗi bảo mật: Không thể khóa tài khoản cấp ADMIN!");
        }

        String detailedMessage = "";

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
                managedStore.setAccount(null);
                accountRepository.saveAndFlush(account);

                replacementAccount.setStore(managedStore);
                managedStore.setAccount(replacementAccount);
                accountRepository.save(replacementAccount);

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

        // BƯỚC 1: THÁO GHẾ CẢ 2 NGƯỜI RA TRƯỚC
        acc1.setStore(null);
        store1.setAccount(null);

        acc2.setStore(null);
        store2.setAccount(null);

        accountRepository.saveAndFlush(acc1);
        accountRepository.saveAndFlush(acc2);

        // BƯỚC 2: TRÁO ĐỔI HỘ KHẨU
        acc1.setStore(store2);
        store2.setAccount(acc1);

        acc2.setStore(store1);
        store1.setAccount(acc2);

        accountRepository.save(acc1);
        accountRepository.save(acc2);

        return "Đã HOÁN ĐỔI VỊ TRÍ thành công! " +
                "Quản lý [" + acc1.getUsername() + "] chuyển sang tiệm [" + store2.getName() + "]. " +
                "Quản lý [" + acc2.getUsername() + "] chuyển sang tiệm [" + store1.getName() + "].";
    }

    // =========================================================================
    // 🌟 NGHIỆP VỤ CẬP NHẬT THÔNG TIN HỒ SƠ (ĐỔI TÊN, EMAIL, PASSWORD)
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public String updateAccountInfo(String accountId, UpdateAccountRequest request) {
        // 1. Tìm Account
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + accountId));

        // 2. Lấy hồ sơ (SystemUser) ra để sửa
        SystemUser profile = account.getSystemUser();
        if (profile == null) {
            throw new RuntimeException("Tài khoản này chưa có hồ sơ nhân sự (SystemUser)!");
        }

        // 3. Cập nhật Họ tên (nếu có truyền lên)
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName().trim());
        }

        // 4. Cập nhật Email (nếu có và phải check trùng lặp)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String cleanEmail = request.getEmail().trim();
            // Kiểm tra xem email mới này có bị ai khác dùng chưa (loại trừ chính mình)
            Optional<SystemUser> existingEmailUser = systemUserRepository.findByEmail(cleanEmail);
            if (existingEmailUser.isPresent() && !existingEmailUser.get().getUserId().equals(profile.getUserId())) {
                throw new RuntimeException("Email này đã được sử dụng cho một tài khoản khác!");
            }
            profile.setEmail(cleanEmail);
        }

        // 5. Đặt lại Mật khẩu (nếu Admin có nhập password mới)
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            account.setPassword(passwordEncoder.encode(request.getNewPassword()));
            accountRepository.save(account); // Lưu account vì đổi password
        }

        // 6. Lưu lại hồ sơ
        systemUserRepository.save(profile);

        return "Đã cập nhật thông tin tài khoản [" + account.getUsername() + "] thành công!";
    }
}