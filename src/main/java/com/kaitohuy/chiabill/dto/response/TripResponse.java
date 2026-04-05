package com.kaitohuy.chiabill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private BigDecimal totalBudget;
    private Long ownerId;
    private LocalDateTime createdAt;
    private List<TripMemberResponse> members;
}