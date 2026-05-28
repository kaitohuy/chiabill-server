package com.kaitohuy.chiabill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundContributionResponse {
    private Long id;
    private Long fundId;
    private UserResponse contributor;
    private BigDecimal amount;
    private LocalDateTime contributionDate;
    private String notes;
    private String type;
    private Boolean isConfirmed;
}
