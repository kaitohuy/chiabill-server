package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Long categoryId;

    @NotNull(message = "Expense date is required")
    private LocalDateTime expenseDate;
    
    @Size(max = 500, message = "Receipt URL must not exceed 500 characters")
    private String receiptUrl;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;
    private BigDecimal exchangeRate;

    private Boolean isFromFund;

    @Size(max = 50, message = "Split type must not exceed 50 characters")
    private String splitType;

    @NotEmpty(message = "Splits cannot be empty")
    @Valid
    private List<SplitRequest> splits;
}

