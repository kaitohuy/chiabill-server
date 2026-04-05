package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.UpdateProfileRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.UserService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getProfile(Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userService.getMyProfile(userId))
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userService.updateProfile(userId, request))
                .build();
    }

    @PostMapping(value = "/avatar", consumes = MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<String>builder()
                .success(true)
                .data(userService.uploadAvatar(userId, file))
                .build();
    }

    @PostMapping(value = "/bank-qr", consumes = MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadBankQr(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<String>builder()
                .success(true)
                .data(userService.uploadBankQr(userId, file))
                .build();
    }
}