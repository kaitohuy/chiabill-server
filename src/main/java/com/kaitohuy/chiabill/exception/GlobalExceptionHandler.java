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
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Invalid request");

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
                        .message(ex.getMessage() != null ? ex.getMessage() : "Internal server error")
                        .build()
        );
    }
}