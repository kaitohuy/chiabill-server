package com.kaitohuy.chiabill.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SearchExpenseRequest {
    private String keyword;
    private Long categoryId;
    private Long payerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
