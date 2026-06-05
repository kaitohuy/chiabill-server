package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.service.interfaces.PlaceSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AdminSeedController {

    private final PlaceSeedService placeSeedService;

    @PostMapping({"/api/v1/admin/seed/places", "/api/admin/seed/places", "/admin/seed/places"})
    public ResponseEntity<ApiResponse<String>> seedPlaces(@RequestBody(required = false) List<PlaceRequest> requests) {
        placeSeedService.seedPlacesAsync(requests);
        return ResponseEntity.accepted().body(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Place seeding started")
                        .build()
        );
    }
}
