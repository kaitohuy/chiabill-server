package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.AppAnnouncementRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.AppAnnouncementResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.entity.AppAnnouncement;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.AppAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AppAnnouncementController {

    private final AppAnnouncementService announcementService;

    /**
     * [PUBLIC - Client gọi khi khởi động app]
     * Lấy danh sách thông báo đang active.
     * Flutter gọi: GET /api/announcements/active?platform=ANDROID
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AppAnnouncementResponse>>> getActiveAnnouncements(
            @RequestParam(defaultValue = "ALL") AppAnnouncement.Platform platform) {

        List<AppAnnouncementResponse> response = announcementService.getActiveAnnouncements(platform);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Lấy thông báo thành công"));
    }

    /**
     * [ADMIN] Xem tất cả thông báo (có phân trang)
     * GET /api/announcements?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AppAnnouncementResponse>>> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<AppAnnouncementResponse> response = announcementService
                .getAllAnnouncements(PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Lấy danh sách thông báo thành công"));
    }

    /**
     * [ADMIN] Tạo thông báo mới
     * POST /api/announcements
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppAnnouncementResponse>> createAnnouncement(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @Valid @RequestBody AppAnnouncementRequest request) {

        AppAnnouncementResponse response = announcementService
                .createAnnouncement(userDetails.getUserId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Tạo thông báo thành công"));
    }

    /**
     * [ADMIN] Chỉnh sửa thông báo
     * PUT /api/announcements/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppAnnouncementResponse>> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AppAnnouncementRequest request) {

        AppAnnouncementResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Cập nhật thông báo thành công"));
    }

    /**
     * [ADMIN] Bật / Tắt nhanh thông báo
     * PATCH /api/announcements/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AppAnnouncementResponse>> toggleActive(@PathVariable Long id) {
        AppAnnouncementResponse response = announcementService.toggleActive(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Đã cập nhật trạng thái thông báo"));
    }

    /**
     * [ADMIN] Xoá thông báo (soft delete)
     * DELETE /api/announcements/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Xoá thông báo thành công"));
    }
}
