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
public class SettlementSummaryResponse {
    private BigDecimal totalOwed;      // Sum of amounts you owe others
    private BigDecimal totalReceivable; // Sum of amounts others owe you
}
