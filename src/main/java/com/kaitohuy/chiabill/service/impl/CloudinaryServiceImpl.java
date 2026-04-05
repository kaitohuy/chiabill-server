package com.kaitohuy.chiabill.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File cannot be empty");
        }

        try {
            // Đặt tên ngẫu nhiên cho file trên Cloudinary để tránh trùng lặp
            String publicId = UUID.randomUUID().toString();
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "chiabill_uploads"
                    ));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Error uploading file to Cloudinary: ", e);
            throw new BusinessException("Could not upload image to Cloudinary");
        }
    }

    @org.springframework.scheduling.annotation.Async
    @Override
    public void deleteImage(String secureUrl) {
        if (secureUrl == null || secureUrl.trim().isEmpty() || !secureUrl.contains("cloudinary.com")) {
            return;
        }

        try {
            // Lấy ra phần Tên thư mục và Tên file để xoá (VD: "chiabill_uploads/df12s-g234fd")
            String[] parts = secureUrl.split("/");
            String filename = parts[parts.length - 1];
            String folder = parts[parts.length - 2];
            String publicId = folder + "/" + filename.substring(0, filename.lastIndexOf('.'));

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted old image from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.error("Could not delete image from Cloudinary: ", e);
        }
    }
}
