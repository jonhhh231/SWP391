package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.auth.AuthRequest;
import com.groupSWP.centralkitchenplatform.dto.auth.AuthResponse;
import com.groupSWP.centralkitchenplatform.dto.auth.RegisterRequest; // DTO mới tạo
import com.groupSWP.centralkitchenplatform.dto.auth.UpdateProfileRequest;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser; // Entity Profile
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.SystemUserRepository; // Repository mới
import com.groupSWP.centralkitchenplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import cho Transaction
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor // Tự động tạo Constructor cho các biến final (Dependency Injection)
public class AuthService {

    // Đổi tên 'repository' -> 'accountRepository' cho rõ nghĩa hơn nhé Sếp
    private final AccountRepository accountRepository;
    private final SystemUserRepository systemUserRepository; // Inject thêm cái này để lưu Profile
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(AuthRequest request) {
        // Tìm account theo username
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // So sánh mật khẩu (Bây giờ chắc chắn khớp 100% vì bước trước đã update rồi)
        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mật khẩu!");
        }

        // Nếu khớp -> Sinh Token trả về
        String token = jwtService.generateToken(account);
        return new AuthResponse(token, account.getUsername(), account.getRole());
    }

    // =========================================================================
    // 2. CHỨC NĂNG ĐĂNG KÝ (REGISTER) - Mới thêm vào
    // =========================================================================
    // @Transactional: Cực quan trọng! Đảm bảo "All or Nothing".
    // Nếu tạo Account xong mà tạo Profile bị lỗi -> Nó sẽ tự hủy (Rollback) cái Account luôn.
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {

        // BƯỚC 1: Kiểm tra xem Username đã tồn tại chưa
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username này đã có người dùng rồi!");
        }

        // BƯỚC 2: Tạo Account (Để đăng nhập)
        Account account = new Account();
        account.setUsername(request.getUsername());
        // Lưu ý: Phải mã hóa mật khẩu trước khi lưu vào DB
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        // Lưu role dạng String để JWT xử lý nhanh
        account.setRole(request.getRole().name());

        // Lưu Account vào DB trước -> Để lấy được ID (hoặc để Hibernate quản lý Persistence Context)
        account = accountRepository.save(account);

        // BƯỚC 3: Tạo SystemUser (Profile chi tiết nhân viên)
        // Dùng Builder Pattern cho gọn code
        SystemUser userProfile = SystemUser.builder()
                .userId(generateStaffId(request.getRole()))       // Tự động sinh mã NV
                .fullName(request.getFullName()) // Lấy tên thật từ request
                .role(request.getRole())         // Lấy Enum Role
                .account(account)                // Quan trọng: Gắn Profile này vào Account vừa tạo ở trên
                .build();

        // Lưu Profile vào DB
        systemUserRepository.save(userProfile);

        return "Đăng ký thành công! Mã nhân viên của bạn là: " + userProfile.getUserId();
    }

    // =========================================================================
    // UTILS - Các hàm phụ trợ
    // =========================================================================

    // Hàm sinh mã nhân viên tự động
    private String generateStaffId(SystemUser.SystemRole role) {
        // 1. Xác định Prefix (Mã chức vụ)
        String prefix = getPrefixByRole(role);

        // 2. Tìm mã nhân viên lớn nhất hiện tại trong DB của Role này
        Optional<String> lastUserId = systemUserRepository.findLastUserIdByRole(role);

        // 3. Nếu chưa có ai -> Đây là người đầu tiên
        if (lastUserId.isEmpty()) {
            return prefix + "00001"; // Format 5 số
        }

        // 4. Nếu đã có -> Lấy số đuôi + 1
        String lastId = lastUserId.get();
        // Cắt bỏ phần Prefix để lấy số (VD: "KIT00009" -> lấy "00009")
        String numberPart = lastId.substring(prefix.length());

        try {
            int number = Integer.parseInt(numberPart);
            number++; // Tăng lên 1
            // Format lại thành chuỗi 5 số (VD: 10 -> "00010")
            return prefix + String.format("%05d", number);
        } catch (NumberFormatException e) {
            // Fallback nếu dữ liệu cũ bị lỗi format
            return prefix + System.currentTimeMillis();
        }
    }

    // Hàm phụ để map Enum sang String ngắn gọn
    private String getPrefixByRole(SystemUser.SystemRole role) {
        if (role == null) return "UNK"; // Unknown (Phòng hờ lỗi null)

        return switch (role) {
            // 1. Admin (Quản trị hệ thống) -> ADM
            case ADMIN -> "ADM";

            // 2. Manager (Quản lý vận hành) -> MNG
            case MANAGER -> "MNG";

            // 3. Supply Coordinator (Điều phối cung ứng) -> COR
            // (Em đổi COD -> COR cho nó chuẩn tiếng Anh Coordinator hơn nha Sếp)
            case COORDINATOR -> "COR";

            // 4. Central Kitchen Staff (Nhân viên bếp) -> KIT
            case KITCHEN_STAFF -> "KIT";

            // 5. Franchise Store Staff (Nhân viên cửa hàng) -> STR
            // (Đây là vai trò mới Sếp vừa thêm)
            case STORE_STAFF -> "STR";

            // Trường hợp lạ (Fallback)
            default -> "USR";
        };
    }

    @Transactional
    public SystemUser updateProfile(String currentUsername, UpdateProfileRequest request) {
        // 1. Tìm Account đang đăng nhập
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // 2. Lấy Profile đi kèm
        SystemUser profile = account.getSystemUser();
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }

        // 3. Cập nhật thông tin (Chỉ update nếu có gửi lên)
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName());
        }

        // Nếu sau này Sếp có thêm field SĐT hay Email thì if tiếp ở đây...

        return systemUserRepository.save(profile);
    }
}