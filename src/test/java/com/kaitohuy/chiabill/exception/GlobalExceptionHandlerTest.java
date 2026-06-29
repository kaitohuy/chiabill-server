package com.kaitohuy.chiabill.exception;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_withErrorCode_shouldReturnErrorCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);
        ResponseEntity<ApiResponse<?>> response = handler.handleBusiness(ex);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("USER_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("Không tìm thấy người dùng", response.getBody().getMessage());
    }

    @Test
    void handleBusiness_withLegacyMessage_shouldMapToErrorCode() {
        BusinessException ex = new BusinessException("User not found");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusiness(ex);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("USER_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("Không tìm thấy người dùng", response.getBody().getMessage());
    }

    @Test
    void handleBusiness_withUnmappedLegacyMessage_shouldReturnUnknownError() {
        BusinessException ex = new BusinessException("Some totally random error message");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusiness(ex);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("UNKNOWN_ERROR", response.getBody().getErrorCode());
        assertEquals("Some totally random error message", response.getBody().getMessage());
    }
}
