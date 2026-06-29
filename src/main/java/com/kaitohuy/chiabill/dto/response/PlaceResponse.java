package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlaceResponse {
    private Long id;
    private String name;
    private String nameEn;
    private String category;
    private Double latitude;
    private Double longitude;
    private String city;
    private String cityEn;
    private String summary;
    private String summaryEn;
    private String ticketPrices;
    private String ticketPricesEn;
    private String openingHours;
    private String openingHoursEn;
    private List<PlaceImageResponse> images;
    private Long creatorId; // If null, it's a system generated place
    private Boolean isUserGenerated;
}
