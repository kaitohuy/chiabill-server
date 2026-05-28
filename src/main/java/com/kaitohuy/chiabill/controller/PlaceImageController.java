package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.PlaceImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceImageController {

    private final PlaceImageService placeImageService;

    @PostMapping("/{placeId}/images")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long placeId,
            @RequestParam("album") String album,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        String imageUrl = placeImageService.uploadImage(placeId, album, file, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, imageUrl, null));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        placeImageService.deleteImage(imageId, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null));
    }
}
