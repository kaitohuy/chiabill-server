package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.SystemFeedbackRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.SystemFeedbackResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.SystemFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class SystemFeedbackController {

    private final SystemFeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<SystemFeedbackResponse>> createFeedback(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @Valid @RequestBody SystemFeedbackRequest request) {

        SystemFeedbackResponse response = feedbackService.saveFeedback(userDetails.getUserId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Gửi phản hồi thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SystemFeedbackResponse>>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<SystemFeedbackResponse> response = feedbackService.getFeedbacks(PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Lấy danh sách phản hồi thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Xóa phản hồi thành công"));
    }
}
