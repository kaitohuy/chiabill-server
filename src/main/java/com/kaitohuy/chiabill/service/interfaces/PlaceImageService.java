package com.kaitohuy.chiabill.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface PlaceImageService {
    String uploadImage(Long placeId, String album, MultipartFile file, Long userId);
    
    void deleteImage(Long imageId, Long userId);
}
