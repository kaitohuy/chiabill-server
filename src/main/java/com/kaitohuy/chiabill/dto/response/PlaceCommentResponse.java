package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlaceCommentResponse {
    private Long id;
    private Long placeId;
    private UserResponse user;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private Boolean isLikedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private java.util.List<PlaceCommentResponse> replies;
}
