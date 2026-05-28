package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.PlaceCommentRequest;
import com.kaitohuy.chiabill.dto.response.PlaceCommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlaceCommentService {
    Page<PlaceCommentResponse> getComments(Long placeId, Long currentUserId, Pageable pageable);
    
    PlaceCommentResponse addComment(Long placeId, PlaceCommentRequest request, Long userId);
    
    PlaceCommentResponse replyComment(Long commentId, PlaceCommentRequest request, Long userId);
    
    void toggleLikeComment(Long commentId, Long userId);
    
    PlaceCommentResponse updateComment(Long commentId, PlaceCommentRequest request, Long userId);
    
    void deleteComment(Long commentId, Long userId);
}
