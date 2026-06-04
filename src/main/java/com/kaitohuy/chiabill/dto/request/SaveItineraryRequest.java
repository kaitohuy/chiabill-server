package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SaveItineraryRequest {
    private Long id; // null khi thêm mới, có giá trị khi cập nhật lẻ

    @NotNull(message = "Day number is required")
    private Integer dayNumber;

    private String timeRange;

    @NotBlank(message = "Activity is required")
    private String activity;

    private String location;
    
    private String note;
    
    private BigDecimal estimatedCost;
}
