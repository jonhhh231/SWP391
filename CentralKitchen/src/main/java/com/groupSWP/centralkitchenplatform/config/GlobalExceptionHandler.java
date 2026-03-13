package com.groupSWP.centralkitchenplatform.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Lớp cấu hình bắt và xử lý ngoại lệ toàn cục (Global Exception Handler) cho toàn bộ hệ thống.
 * <p>
 * Lớp này sử dụng {@link RestControllerAdvice} để can thiệp vào quá trình trả về phản hồi
 * của mọi Controller. Khi có lỗi xảy ra (Validation, Business Logic, Security...), nó sẽ
 * đánh chặn lỗi đó và định dạng lại thành một cấu trúc JSON thống nhất, thân thiện với Frontend.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt và xử lý các lỗi liên quan đến xác thực dữ liệu đầu vào (Validation).
     * <p>
     * Thường xảy ra khi client gửi request body không thỏa mãn các điều kiện ràng buộc
     * (như {@code @Valid}, {@code @NotNull}, {@code @NotBlank}, {@code @Positive}...)
     * trong các class DTO.
     * </p>
     *
     * @param ex Ngoại lệ {@link MethodArgumentNotValidException} chứa danh sách các trường bị lỗi.
     * @return Phản hồi HTTP 400 (Bad Request) với body là một Map chứa cặp [Tên trường : Thông báo lỗi].
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Bắt và xử lý các lỗi vi phạm logic nghiệp vụ (Business Rules).
     * <p>
     * Thường xảy ra khi các lớp Service chủ động ném ra {@link IllegalArgumentException}
     * để chặn các thao tác không hợp lệ (ví dụ: "Số lượng không đủ", "Mã đơn hàng không tồn tại").
     * </p>
     *
     * @param ex Ngoại lệ {@link IllegalArgumentException} chứa thông báo lỗi chi tiết.
     * @return Phản hồi HTTP 400 (Bad Request) với cấu trúc JSON: {@code {"message": "Lý do lỗi"}}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBusinessExceptions(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Bắt và xử lý các lỗi runtime chung phát sinh trong quá trình chạy ứng dụng.
     * <p>
     * Đóng vai trò như một màng lọc dự phòng để bắt các ngoại lệ {@link RuntimeException}
     * mà các lập trình viên chủ động ném ra trong code (throw new RuntimeException("...")).
     * </p>
     *
     * @param ex Ngoại lệ {@link RuntimeException} chứa thông báo lỗi.
     * @return Phản hồi HTTP 400 (Bad Request) với cấu trúc JSON: {@code {"error": "Bad Request", "message": "..."}}.
     */
    @ExceptionHandler(RuntimeException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> handleRuntimeException(RuntimeException ex) {
        java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Bắt và xử lý các lỗi liên quan đến phân quyền (Authorization) của Spring Security.
     * <p>
     * Xảy ra khi một người dùng đã đăng nhập (có Token hợp lệ) nhưng lại cố gắng truy cập
     * vào một API vượt quá thẩm quyền của họ (bị chặn bởi {@code @PreAuthorize}).
     * </p>
     *
     * @param ex Ngoại lệ {@link AccessDeniedException} do Spring Security ném ra.
     * @return Phản hồi HTTP 403 (Forbidden) với cấu trúc JSON cảnh báo từ chối truy cập.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", "Bạn không có quyền truy cập chức năng này (Access Denied)!");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
}