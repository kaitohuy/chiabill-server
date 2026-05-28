package com.kaitohuy.chiabill.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateTripRequest {
    private String name;
    private String description;
    private String coverUrl;
    private BigDecimal totalBudget;
    private java.time.LocalDateTime startDate;
    private String categoryName;
    private String categoryIcon;
}