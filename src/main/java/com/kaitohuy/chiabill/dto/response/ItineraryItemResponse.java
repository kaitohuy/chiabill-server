package com.kaitohuy.chiabill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryItemResponse {
    private Long id;
    private Integer dayNumber;
    private String timeRange;
    private String activity;
    private String location;
    private String note;
    private BigDecimal estimatedCost;
}
