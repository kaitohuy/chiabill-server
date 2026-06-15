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
public class ScanReceiptResponse {
    private BigDecimal totalAmount;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String expenseDate;
}
