package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.dto.response.UserStatsResponse;
import com.kaitohuy.chiabill.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        UserStatsResponse stats = userService.getUserStats();
        return ResponseEntity.ok(new ApiResponse<>(true, stats, "Lấy thống kê người dùng thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        Page<UserResponse> users = userService.searchUsers(keyword, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, users, "Lấy danh sách người dùng thành công"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.adminDeleteUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Xóa tài khoản người dùng thành công"));
    }
}
