package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.UpdateProfileRequest;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.exception.ErrorCode;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getBankId() != null) {
            String bankId = request.getBankId().trim();
            user.setBankId(bankId.isEmpty() ? null : bankId);
        }

        if (request.getAccountNo() != null) {
            String accountNo = request.getAccountNo().trim();
            user.setAccountNo(accountNo.isEmpty() ? null : accountNo);
        }

        if (request.getPaymentPriority() != null) {
            user.setPaymentPriority(request.getPaymentPriority());
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }

        if (request.getAllowAutoAdd() != null) {
            // Nếu muốn tắt tự động add (false), phải đảm bảo user có email để nhận thư mời
            if (!request.getAllowAutoAdd() && (user.getEmail() == null || user.getEmail().isBlank())) {
                throw new BusinessException(ErrorCode.EMAIL_NOT_UPDATED);
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

        if (request.getLanguage() != null) {
            String lang = request.getLanguage().trim().toLowerCase();
            if ("vi".equals(lang) || "en".equals(lang)) {
                user.setLanguage(lang);
            }
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getBankQrUrl() != null) {
            cloudinaryService.deleteImage(user.getBankQrUrl());
        }

        String bankQrUrl = cloudinaryService.uploadImage(file);
        user.setBankQrUrl(bankQrUrl);
        userRepository.save(user);

        return bankQrUrl;
    }

    @Override
    @Transactional
    public void deleteMyAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        performSoftDelete(user);
    }

    @Override
    @Transactional
    public void deleteAccountByEmailOrPhone(String email, String phone) {
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new BusinessException(ErrorCode.EMAIL_OR_PHONE_REQUIRED);
        }
        User user = userRepository.findByEmailOrPhone(
                (email != null && !email.isBlank()) ? email.trim() : null,
                (phone != null && !phone.isBlank()) ? phone.trim() : null
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        performSoftDelete(user);
    }

    private void performSoftDelete(User user) {
        // Xoá ảnh trên Cloudinary nếu có để tiết kiệm dung lượng
        if (user.getAvatarUrl() != null) {
            try {
                cloudinaryService.deleteImage(user.getAvatarUrl());
            } catch (Exception e) {
                // Bỏ qua lỗi xóa ảnh
            }
            user.setAvatarUrl(null);
        }
        if (user.getBankQrUrl() != null) {
            try {
                cloudinaryService.deleteImage(user.getBankQrUrl());
            } catch (Exception e) {
                // Bỏ qua lỗi xóa ảnh
            }
            user.setBankQrUrl(null);
        }

        // Soft delete bằng cách set isDeleted và scramble các trường unique/nhạy cảm
        user.setIsDeleted(true);
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        if (user.getEmail() != null) {
            user.setEmail("deleted_" + timestamp + "_" + user.getEmail());
        }
        
        user.setPhone(null);
        user.setName("Người dùng đã xóa");
        user.setProviderId(null);
        user.setAccountNo(null);
        user.setBankId(null);
        
        userRepository.save(user);
    }

    @Override
    public com.kaitohuy.chiabill.dto.response.UserStatsResponse getUserStats() {
        return com.kaitohuy.chiabill.dto.response.UserStatsResponse.builder()
                .totalUsers(userRepository.countByIsDeletedFalse())
                .ghostUsers(userRepository.countByIsGhostTrueAndIsDeletedFalse())
                .anonymousUsers(userRepository.countByIsAnonymousTrueAndIsDeletedFalse())
                .activeUsers(userRepository.countByIsAnonymousFalseAndIsGhostFalseAndIsDeletedFalse())
                .build();
    }

    @Override
    public org.springframework.data.domain.Page<UserResponse> searchUsers(String keyword, org.springframework.data.domain.Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void adminDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        performSoftDelete(user);
    }
}