package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.dto.response.PlaceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlaceService {
    Page<PlaceResponse> getPlaces(String category, Pageable pageable);
    
    Page<PlaceResponse> searchPlaces(String keyword, Pageable pageable);
    
    List<PlaceResponse> getPlacesNearby(double latitude, double longitude, double radiusInKm, int limit);
    
    PlaceResponse getPlaceById(Long id);
    
    PlaceResponse createPlace(PlaceRequest request, Long creatorId);

    List<PlaceResponse> createPlaces(List<PlaceRequest> requests, Long creatorId);
    
    PlaceResponse updatePlace(Long id, PlaceRequest request, Long userId);
    
    void deletePlace(Long id, Long userId);
}
