package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.PlaceCommentRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.PlaceCommentResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.PlaceCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceCommentController {

    private final PlaceCommentService placeCommentService;

    @GetMapping("/{placeId}/comments")
    public ResponseEntity<ApiResponse<Page<PlaceCommentResponse>>> getComments(
            @PathVariable Long placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        return ResponseEntity.ok(new ApiResponse<>(true, placeCommentService.getComments(placeId, currentUserId, PageRequest.of(page, size)), null));
    }

    @PostMapping("/{placeId}/comments")
    public ResponseEntity<ApiResponse<PlaceCommentResponse>> addComment(
            @PathVariable Long placeId,
            @Valid @RequestBody PlaceCommentRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeCommentService.addComment(placeId, request, userDetails.getUserId()), null));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<PlaceCommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody PlaceCommentRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeCommentService.updateComment(commentId, request, userDetails.getUserId()), null));
    }

    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<ApiResponse<PlaceCommentResponse>> replyComment(
            @PathVariable Long commentId,
            @Valid @RequestBody PlaceCommentRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        return ResponseEntity.ok(new ApiResponse<>(true, placeCommentService.replyComment(commentId, request, userDetails.getUserId()), null));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> toggleLikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        placeCommentService.toggleLikeComment(commentId, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        
        placeCommentService.deleteComment(commentId, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null));
    }
}
