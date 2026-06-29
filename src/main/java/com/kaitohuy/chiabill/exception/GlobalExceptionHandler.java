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
        
        String errorCode = "UNKNOWN_ERROR";
        String message = ex.getMessage();
        
        if (ex.getErrorCode() != null && ex.getErrorCode() != ErrorCode.UNKNOWN_ERROR) {
            errorCode = ex.getErrorCode().getCode();
            message = ex.getErrorCode().getDefaultMessage();
        } else {
            // Tương thích ngược: Thử ánh xạ chuỗi tin nhắn thô cũ sang ErrorCode mới
            ErrorCode mappedCode = mapLegacyMessageToErrorCode(ex.getMessage());
            if (mappedCode != null) {
                errorCode = mappedCode.getCode();
                message = mappedCode.getDefaultMessage();
            }
        }
        
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .message(message)
                        .errorCode(errorCode)
                        .build()
        );
    }

    private ErrorCode mapLegacyMessageToErrorCode(String message) {
        if (message == null) return null;
        switch (message) {
            case "User not found":
                return ErrorCode.USER_NOT_FOUND;
            case "Trip not found":
                return ErrorCode.TRIP_NOT_FOUND;
            case "Actor not found":
                return ErrorCode.ACTOR_NOT_FOUND;
            case "User already in trip":
            case "Người dùng đã là thành viên của chuyến đi.":
                return ErrorCode.USER_ALREADY_IN_TRIP;
            case "Owner not found":
                return ErrorCode.OWNER_NOT_FOUND;
            case "Creditor not found":
                return ErrorCode.CREDITOR_NOT_FOUND;
            case "User not in trip":
            case "Bạn không thuộc chuyến đi này":
                return ErrorCode.USER_NOT_IN_TRIP;
            case "User is not active in trip":
                return ErrorCode.USER_NOT_ACTIVE_IN_TRIP;
            case "Trip has been deleted":
                return ErrorCode.TRIP_HAS_BEEN_DELETED;
            case "Access denied: not a member of this trip":
                return ErrorCode.ACCESS_DENIED_NOT_MEMBER;
            case "Access denied":
            case "Không có quyền truy cập":
                return ErrorCode.ACCESS_DENIED;
            default:
                return null;
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
                        .errorCode("VALIDATION_ERROR")
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
                        .errorCode("SYSTEM_ERROR")
                        .build()
        );
    }
}