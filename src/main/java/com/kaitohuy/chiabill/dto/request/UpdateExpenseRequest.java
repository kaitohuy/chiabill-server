package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateExpenseRequest {

    @NotNull(message = "Payer ID is required")
    private Long payerId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    private String description;

    private Long categoryId;

    @NotNull(message = "Expense date is required")
    private LocalDateTime expenseDate;
    
    private String receiptUrl;

    @NotEmpty(message = "Splits cannot be empty")
    @Valid
    private List<SplitRequest> splits;
}
