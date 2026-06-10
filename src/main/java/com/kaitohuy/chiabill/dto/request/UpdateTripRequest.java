package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateTripRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Size(max = 500, message = "Cover URL must not exceed 500 characters")
    private String coverUrl;

    private BigDecimal totalBudget;

    private java.time.LocalDateTime startDate;
    private java.time.LocalDateTime endDate;

    @Size(max = 50, message = "Category name must not exceed 50 characters")
    private String categoryName;
    
    @Size(max = 50, message = "Category icon must not exceed 50 characters")
    private String categoryIcon;
}

