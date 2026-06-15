package com.kaitohuy.chiabill.exception;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusiness(BusinessException ex) {
        log.warn("Business Exception: {}", ex.getMessage());
        String translatedMessage = translateMessage(ex.getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .message(translatedMessage)
                        .build()
        );
    }

    private String translateMessage(String message) {
        if (message == null) return "";
        switch (message) {
            case "User not found":
                return "Không tìm thấy người dùng";
            case "Trip not found":
                return "Không tìm thấy chuyến đi";
            case "Actor not found":
                return "Không tìm thấy người thực hiện";
            case "User already in trip":
                return "Người dùng đã là thành viên của chuyến đi";
            case "Owner not found":
                return "Không tìm thấy chủ nhóm";
            case "Creditor not found":
                return "Không tìm thấy chủ nợ";
            case "User not in trip":
                return "Người dùng không thuộc chuyến đi này";
            case "User is not active in trip":
                return "Người dùng không hoạt động trong chuyến đi này";
            case "Trip has been deleted":
                return "Chuyến đi đã bị xóa";
            case "Access denied: not a member of this trip":
                return "Không có quyền truy cập: bạn không phải thành viên của chuyến đi";
            case "Access denied":
                return "Không có quyền truy cập";
            default:
                return message;
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Yêu cầu không hợp lệ");

        log.warn("Validation Error: {}", message);

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .message(message)
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Internal Server Error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.builder()
                        .success(false)
                        .message("Đã có lỗi xảy ra trên hệ thống")
                        .build()
        );
    }
}