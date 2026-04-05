package com.kaitohuy.chiabill.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExpenseResponse {

    private Long id;
    private Long tripId;
    private BigDecimal totalAmount;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String currency;
    private LocalDateTime expenseDate;

    private UserResponse payer;

    private List<SplitResponse> splits;
}