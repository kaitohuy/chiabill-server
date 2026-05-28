package com.kaitohuy.chiabill.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RequiredContributionRequest {
    private BigDecimal amount;
    private String notes;
    private List<Long> contributorIds;
}
