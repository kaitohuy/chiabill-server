package com.kaitohuy.chiabill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripStatResponse {
    private Long tripId;
    private String tripName;
    private BigDecimal totalAmount;
    private String categoryIcon;
}
