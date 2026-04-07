package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateTripRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;
    
    private String coverUrl;

    private BigDecimal totalBudget;
}
