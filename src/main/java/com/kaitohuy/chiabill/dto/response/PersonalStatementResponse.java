package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PersonalStatementResponse {
    private Long userId;
    private String userName;
    private BigDecimal totalPaid;
    private BigDecimal totalSpent;
    private BigDecimal netBalance;
    private List<ExpenseResponse> involvedExpenses;
    private List<PaymentResponse> involvedPayments;
}
