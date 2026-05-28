package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceImage;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.PlaceImageRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import com.kaitohuy.chiabill.service.interfaces.PlaceImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PlaceImageServiceImpl implements PlaceImageService {

    private final PlaceImageRepository placeImageRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public String uploadImage(Long placeId, String album, MultipartFile file, Long userId) {
        Place place = placeRepository.findByIdAndIsDeletedFalse(placeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy địa điểm"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        // Kiểm tra giới hạn: 1 user chỉ được upload max 10 ảnh cho 1 địa điểm (tránh spam/DDOS)
        int uploadedCount = placeImageRepository.countByPlaceIdAndUserIdAndIsDeletedFalse(placeId, userId);
        if (uploadedCount >= 10) {
            throw new BusinessException("Bạn đã đạt giới hạn tải lên tối đa 10 ảnh cho địa điểm này.");
        }

        String imageUrl = cloudinaryService.uploadImage(file);
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException("Lỗi tải ảnh lên hệ thống.");
        }

        PlaceImage placeImage = PlaceImage.builder()
                .place(place)
                .user(user)
                .album(album)
                .imageUrl(imageUrl)
                .build();
                
        placeImageRepository.save(placeImage);
        return imageUrl;
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId, Long userId) {
        PlaceImage placeImage = placeImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hình ảnh"));

        if (placeImage.getIsDeleted() || !placeImage.getUser().getId().equals(userId)) {
            throw new BusinessException("Không có quyền xóa ảnh này");
        }

        // Xóa ảnh trên Cloudinary
        cloudinaryService.deleteImage(placeImage.getImageUrl());
        
        placeImage.setIsDeleted(true);
        placeImageRepository.save(placeImage);
    }
}
