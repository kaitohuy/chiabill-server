package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class PlaceRequest {
    @NotBlank(message = "Tên địa điểm không được để trống")
    private String name;
    private String nameEn;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    private Double latitude;

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    private Double longitude;

    private String city;
    private String cityEn;
    private String summary;
    private String summaryEn;
    private String ticketPrices;
    private String ticketPricesEn;
    private String openingHours;
    private String openingHoursEn;
    
    private List<String> imageUrls;
}
