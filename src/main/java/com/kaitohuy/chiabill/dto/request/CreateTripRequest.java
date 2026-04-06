package com.kaitohuy.chiabill.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateTripRequest {
    private String name;
    private String description;
    private BigDecimal totalBudget;
}