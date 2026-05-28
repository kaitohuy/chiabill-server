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
public class FundResponse {
    private Long id;
    private Long tripId;
    private BigDecimal balance;
    private String currency;
    private BigDecimal alertThreshold;
    private UserResponse treasurer;
}
