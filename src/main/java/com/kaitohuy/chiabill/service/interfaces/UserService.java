package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.UpdateProfileRequest;
import com.kaitohuy.chiabill.dto.response.UserResponse;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse getMyProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    String uploadAvatar(Long userId, MultipartFile file);

    String uploadBankQr(Long userId, MultipartFile file);

    void deleteMyAccount(Long userId);

    void deleteAccountByEmailOrPhone(String email, String phone);

    com.kaitohuy.chiabill.dto.response.UserStatsResponse getUserStats();

    org.springframework.data.domain.Page<com.kaitohuy.chiabill.dto.response.UserResponse> searchUsers(String keyword, org.springframework.data.domain.Pageable pageable);

    void adminDeleteUser(Long userId);
}