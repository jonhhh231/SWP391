package com.groupSWP.centralkitchenplatform.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BẮT LỖI DỮ LIỆU ĐẦU VÀO (@Valid, @NotNull, @Positive...)
     * Lỗi 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Lôi từng lỗi ra và chỉ lấy đúng Tên trường (Field) + Thông báo (Message)
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * BẮT LỖI LOGIC NGHIỆP VỤ (Các lỗi quăng ra từ hàm Service)
     * Lỗi 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBusinessExceptions(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage()); // Lấy câu thông báo từ throw new IllegalArgumentException(...)

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * BẮT TOÀN BỘ LỖI CHUNG (Các lỗi anh em mình chủ động quăng ra bằng throw new RuntimeException)
     * Lỗi 400 Bad Request
     */
    @ExceptionHandler(RuntimeException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> handleRuntimeException(RuntimeException ex) {
        java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}