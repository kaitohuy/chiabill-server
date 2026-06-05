package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class StaticPagesController {

    private final UserService userService;

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_HTML_VALUE)
    public String getPrivacyPolicy() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/privacy-policy.html");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<h2>Không thể tải trang Chính sách bảo mật. Vui lòng quay lại sau!</h2>";
        }
    }

    @GetMapping(value = "/delete-account-request", produces = MediaType.TEXT_HTML_VALUE)
    public String getDeleteAccountRequestPage() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/delete-account-request.html");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<h2>Không thể tải trang Yêu cầu xóa tài khoản. Vui lòng quay lại sau!</h2>";
        }
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
