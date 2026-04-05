package com.kaitohuy.chiabill.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    String uploadImage(MultipartFile file);

    void deleteImage(String secureUrl);
}
