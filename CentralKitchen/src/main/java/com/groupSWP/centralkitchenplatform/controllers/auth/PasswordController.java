package com.groupSWP.centralkitchenplatform.controllers.auth;

import com.groupSWP.centralkitchenplatform.dto.auth.ChangePasswordRequest;
import com.groupSWP.centralkitchenplatform.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller xử lý các thiết lập tài khoản cá nhân của người dùng.
 * <p>
 * Lớp này cung cấp các API liên quan đến cấu hình bảo mật tài khoản,
 * điển hình là chức năng Đổi mật khẩu chủ động khi người dùng đang đăng nhập.
 * Mọi API trong đây đều yêu cầu Request phải đi kèm JWT Token hợp lệ.
 * </p>
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor // 🌟 TỐI ƯU: Dùng Lombok để tự động tiêm PasswordService cho code sạch sẽ
public class PasswordController {

    private final PasswordService passwordService;

    /**
     * API Đổi mật khẩu chủ động.
     * <p>
     * Cho phép người dùng tự thay đổi mật khẩu của mình. API sử dụng {@link Principal}
     * ("Thẻ căn cước" của hệ thống) để định danh chính xác người dùng hiện tại,
     * ngăn chặn tuyệt đối việc hacker truyền ID giả để đổi mật khẩu của người khác.
     * </p>
     * <p>
     * <b>Kiểm soát lỗi:</b> Các vi phạm logic (như nhập sai mật khẩu cũ, mật khẩu mới không khớp)
     * sẽ được {@code PasswordService} ném ra ngoại lệ và được {@code GlobalExceptionHandler}
     * đánh chặn tự động để trả về chuẩn JSON 400 Bad Request.
     * </p>
     *
     * @param principal Đối tượng bảo mật chứa danh tính (username) của người dùng trích xuất từ Token.
     * @param request   Payload chứa mật khẩu hiện tại và mật khẩu mới thiết lập.
     * @return Phản hồi HTTP 200 kèm thông báo đổi mật khẩu thành công.
     */
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Principal principal,
            @RequestBody ChangePasswordRequest request) {

        // Lấy username từ Token của người đang đăng nhập
        String username = principal.getName();

        // 🌟 TỐI ƯU: Bỏ try-catch!
        // Cứ gọi thẳng Service, nếu sai pass cũ, Service sẽ throw Exception
        // và GlobalExceptionHandler sẽ tự động tóm lấy nó để chuyển thành JSON lỗi cực mượt!
        passwordService.changePassword(username, request);

        return ResponseEntity.ok("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
    }
}