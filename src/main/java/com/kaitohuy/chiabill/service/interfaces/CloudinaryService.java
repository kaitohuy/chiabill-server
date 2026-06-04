package com.kaitohuy.chiabill.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    String uploadImage(MultipartFile file);

    String uploadImageFromUrl(String imageUrl);

    void deleteImage(String secureUrl);
}
