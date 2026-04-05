package com.kaitohuy.chiabill.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateExpenseRequest {

    private Long tripId;
    private Long payerId;

    private BigDecimal totalAmount;

    private String description;
    private Long categoryId;

    private LocalDateTime expenseDate;

    private List<SplitRequest> splits;
}