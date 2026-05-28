package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceReportRequest {
    @NotBlank(message = "Loại báo cáo không được để trống")
    private String reportType;
    
    private String description;
}
