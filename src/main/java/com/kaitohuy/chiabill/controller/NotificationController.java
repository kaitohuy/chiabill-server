package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.RegisterTokenRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.NotificationResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.NotificationService;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping("/register-token")
    public ApiResponse<?> registerToken(
            @RequestBody RegisterTokenRequest request,
            Authentication authentication) {
        
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        User user = userRepository.findById(userId).orElseThrow();
        
        notificationService.registerToken(user, request.getToken(), request.getPlatform());
        
        return ApiResponse.builder()
                .success(true)
                .message("Token registered successfully")
                .build();
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .data(notificationService.getNotifications(userId))
                .build();
    }

    @PutMapping("/{id}/read")
    public ApiResponse<?> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        notificationService.markAsRead(id, userId);
        return ApiResponse.builder()
                .success(true)
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ApiResponse.<Long>builder()
                .success(true)
                .data(notificationService.getUnreadCount(userId))
                .build();
    }
}
