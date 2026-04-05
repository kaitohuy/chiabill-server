package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.UpdateProfileRequest;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.UserMapper;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.UserService;

import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public UserResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getBankId() != null) {
            user.setBankId(request.getBankId());
        }

        if (request.getAccountNo() != null) {
            user.setAccountNo(request.getAccountNo());
        }

        if (request.getPaymentPriority() != null) {
            user.setPaymentPriority(request.getPaymentPriority());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getAllowAutoAdd() != null) {
            // Nếu muốn tắt tự động add (false), phải đảm bảo user có email để nhận thư mời
            if (!request.getAllowAutoAdd() && (user.getEmail() == null || user.getEmail().isBlank())) {
                throw new BusinessException("Bạn phải cập nhật Email trước khi tắt quyền tự động thêm vào nhóm mẫu.");
            }
            user.setAllowAutoAdd(request.getAllowAutoAdd());
        }

        if (request.getAllowAutoApprovePayment() != null) {
            user.setAllowAutoApprovePayment(request.getAllowAutoApprovePayment());
        }

        if (request.getAvatarUrl() != null) {
            String newAvatar = request.getAvatarUrl().trim();
            if (newAvatar.isEmpty()) {
                if (user.getAvatarUrl() != null) {
                    cloudinaryService.deleteImage(user.getAvatarUrl());
                }
                user.setAvatarUrl(null);
            } else {
                user.setAvatarUrl(newAvatar);
            }
        }

        if (request.getBankQrUrl() != null) {
            String newBankQr = request.getBankQrUrl().trim();
            if (newBankQr.isEmpty()) {
                if (user.getBankQrUrl() != null) {
                    cloudinaryService.deleteImage(user.getBankQrUrl());
                }
                user.setBankQrUrl(null);
            } else {
                user.setBankQrUrl(newBankQr);
            }
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.getAvatarUrl() != null) {
            cloudinaryService.deleteImage(user.getAvatarUrl());
        }

        String avatarUrl = cloudinaryService.uploadImage(file);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    @Override
    @Transactional
    public String uploadBankQr(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.getBankQrUrl() != null) {
            cloudinaryService.deleteImage(user.getBankQrUrl());
        }

        String bankQrUrl = cloudinaryService.uploadImage(file);
        user.setBankQrUrl(bankQrUrl);
        userRepository.save(user);

        return bankQrUrl;
    }
}