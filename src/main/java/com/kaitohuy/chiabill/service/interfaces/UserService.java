package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.UpdateProfileRequest;
import com.kaitohuy.chiabill.dto.response.UserResponse;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse getMyProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    String uploadAvatar(Long userId, MultipartFile file);

    String uploadBankQr(Long userId, MultipartFile file);
}