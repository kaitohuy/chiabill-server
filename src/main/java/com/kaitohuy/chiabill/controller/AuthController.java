package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.GoogleLoginRequest;
import com.kaitohuy.chiabill.dto.response.*;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/anonymous")
    public ApiResponse<AuthResponse> anonymousLogin() {
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(authService.loginAnonymous())
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> getMe(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(authService.getCurrentUser(userId))
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(
            @RequestBody GoogleLoginRequest request,
            Authentication authentication) {

        Long currentUserId = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            currentUserId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        }

        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(authService.loginGoogle(request.getIdToken(), currentUserId))
                .build();
    }
}