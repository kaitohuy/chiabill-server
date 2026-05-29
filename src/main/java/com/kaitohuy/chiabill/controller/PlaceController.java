package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.dto.request.PlaceReportRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.PlaceResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.PlaceReportService;
import com.kaitohuy.chiabill.service.interfaces.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final PlaceReportService placeReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlaceResponse>>> getPlaces(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeService.getPlaces(category, PageRequest.of(page, size)), null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PlaceResponse>>> searchPlaces(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeService.searchPlaces(keyword, PageRequest.of(page, size)), null));
    }

    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> getPlacesNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "50.0") double radius,
            @RequestParam(defaultValue = "100") int limit) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeService.getPlacesNearby(latitude, longitude, radius, limit), null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceResponse>> getPlaceById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, placeService.getPlaceById(id), null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlaceResponse>> createPlace(
            @Valid @RequestBody PlaceRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeService.createPlace(request, userDetails.getUserId()), null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceResponse>> updatePlace(
            @PathVariable Long id,
            @Valid @RequestBody PlaceRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeService.updatePlace(id, request, userDetails.getUserId()), null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        placeService.deletePlace(id, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ApiResponse<Void>> submitReport(
            @PathVariable Long id,
            @Valid @RequestBody PlaceReportRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        placeReportService.submitReport(id, request, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> createPlaces(
            @Valid @RequestBody List<PlaceRequest> requests,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        placeService.createPlaces(requests, userDetails.getUserId()),
                        null
                )
        );
    }
}
