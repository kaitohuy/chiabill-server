package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PlaceImageResponse {
    private Long id;
    private String imageUrl;
    private String album;
    private Long userId;
    private LocalDateTime createdAt;
}
