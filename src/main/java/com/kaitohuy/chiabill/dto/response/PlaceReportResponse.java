package com.kaitohuy.chiabill.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceReportResponse {
    private Long id;
    private Long placeId;
    private String placeName;
    private String placeCategory;
    private String placeCity;
    private Long userId;
    private String userName;
    private String reportType;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
