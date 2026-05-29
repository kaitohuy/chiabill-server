package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.dto.response.PlaceResponse;
import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceImage;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.PlaceImageRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final UserRepository userRepository;

    @Override
    public Page<PlaceResponse> getPlaces(String category, Pageable pageable) {
        Page<Place> places;
        if (category != null && !category.isEmpty()) {
            places = placeRepository.findAllByIsDeletedFalseAndCategory(category, pageable);
        } else {
            places = placeRepository.findAllByIsDeletedFalse(pageable);
        }
        return places.map(this::mapToResponse);
    }

    @Override
    public Page<PlaceResponse> searchPlaces(String keyword, Pageable pageable) {
        Page<Place> places = placeRepository.findAllByIsDeletedFalseAndNameContainingIgnoreCase(keyword, pageable);
        return places.map(this::mapToResponse);
    }

    @Override
    public List<PlaceResponse> getPlacesNearby(double latitude, double longitude, double radiusInKm, int limit) {
        List<Place> places = placeRepository.findPlacesNearby(latitude, longitude, radiusInKm, limit);
        return places.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public PlaceResponse getPlaceById(Long id) {
        Place place = placeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy địa điểm"));
        return mapToResponse(place);
    }

    @Override
    @Transactional
    public PlaceResponse createPlace(PlaceRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        Place place = Place.builder()
                .name(request.getName())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .city(request.getCity())
                .summary(request.getSummary())
                .ticketPrices(request.getTicketPrices())
                .openingHours(request.getOpeningHours())
                .creator(creator)
                .build();

        place = placeRepository.save(place);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String url : request.getImageUrls()) {
                PlaceImage img = PlaceImage.builder()
                        .place(place)
                        .imageUrl(url)
                        .album("Khác")
                        .user(creator)
                        .build();
                placeImageRepository.save(img);
            }
        }

        return mapToResponse(place);
    }

    @Override
    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceRequest request, Long userId) {
        Place place = placeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy địa điểm"));

        // Check permission: Only creator can update. Admin logic can be added later.
        if (place.getCreator() == null || !place.getCreator().getId().equals(userId)) {
            throw new BusinessException("Không có quyền chỉnh sửa");
        }

        place.setName(request.getName());
        place.setCategory(request.getCategory());
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setCity(request.getCity());
        place.setSummary(request.getSummary());
        place.setTicketPrices(request.getTicketPrices());
        place.setOpeningHours(request.getOpeningHours());

        // Update images: Simple approach - delete old, add new
        List<PlaceImage> oldImages = placeImageRepository.findAllByPlaceIdAndIsDeletedFalse(id);
        placeImageRepository.deleteAll(oldImages);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String url : request.getImageUrls()) {
                PlaceImage img = PlaceImage.builder()
                        .place(place)
                        .imageUrl(url)
                        .album("Khác") // Default album cho seed data
                        .user(place.getCreator()) // Gắn tạm cho người tạo địa điểm
                        .build();
                placeImageRepository.save(img);
            }
        }

        return mapToResponse(placeRepository.save(place));
    }

    @Override
    @Transactional
    public void deletePlace(Long id, Long userId) {
        Place place = placeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy địa điểm"));

        if (place.getCreator() == null || !place.getCreator().getId().equals(userId)) {
            throw new BusinessException("Không có quyền xóa");
        }

        place.setIsDeleted(true);
        placeRepository.save(place);
    }

    private PlaceResponse mapToResponse(Place place) {
        List<com.kaitohuy.chiabill.dto.response.PlaceImageResponse> images = placeImageRepository.findAllByPlaceIdAndIsDeletedFalse(place.getId())
                .stream().map(img -> com.kaitohuy.chiabill.dto.response.PlaceImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .album(img.getAlbum())
                        .userId(img.getUser() != null ? img.getUser().getId() : null)
                        .createdAt(img.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .category(place.getCategory())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .city(place.getCity())
                .summary(place.getSummary())
                .ticketPrices(place.getTicketPrices())
                .openingHours(place.getOpeningHours())
                .images(images)
                .creatorId(place.getCreator() != null ? place.getCreator().getId() : null)
                .isUserGenerated(place.getCreator() != null)
                .build();
    }


    @Override
    @Transactional
    public List<PlaceResponse> createPlaces(List<PlaceRequest> requests, Long creatorId) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("Danh sách địa điểm không được để trống");
        }

        if (requests.size() > 500) {
            throw new BusinessException("Chỉ được import tối đa 500 địa điểm mỗi lần");
        }

        return requests.stream()
                .map(request -> createPlace(request, creatorId))
                .collect(Collectors.toList());
    }
}
