package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlaceResponse {
    private Long id;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private String city;
    private String summary;
    private String ticketPrices;
    private String openingHours;
    private List<PlaceImageResponse> images;
    private Long creatorId; // If null, it's a system generated place
    private Boolean isUserGenerated;
}
