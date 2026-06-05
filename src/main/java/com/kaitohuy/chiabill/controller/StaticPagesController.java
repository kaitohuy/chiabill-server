package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
@RequiredArgsConstructor
public class StaticPagesController {

    private final UserService userService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping(value = "/privacy-policy")
    public ResponseEntity<Void> getPrivacyPolicy() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/privacy-policy"))
                .build();
    }

    @GetMapping(value = "/delete-account-request")
    public ResponseEntity<Void> getDeleteAccountRequestPage() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/delete-account-request"))
                .build();
    }

    @PostMapping(value = "/delete-account-request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> processDeleteAccountRequest(@RequestBody DeleteRequestDto request) {
        try {
            userService.deleteAccountByEmailOrPhone(request.getEmail(), request.getPhone());
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Yêu cầu xóa tài khoản đã được xử lý thành công")
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa tài khoản")
                    .build();
        }
    }

    @Getter
    @Setter
    public static class DeleteRequestDto {
        private String email;
        private String phone;
        private String reason;
    }
}
