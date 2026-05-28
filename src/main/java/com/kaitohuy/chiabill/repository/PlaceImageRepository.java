package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    List<PlaceImage> findAllByPlaceIdAndIsDeletedFalse(Long placeId);
    
    int countByPlaceIdAndUserIdAndIsDeletedFalse(Long placeId, Long userId);
}
