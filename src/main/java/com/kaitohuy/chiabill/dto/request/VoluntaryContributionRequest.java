package com.kaitohuy.chiabill.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoluntaryContributionRequest {
    private BigDecimal amount;
    private String notes;
}
