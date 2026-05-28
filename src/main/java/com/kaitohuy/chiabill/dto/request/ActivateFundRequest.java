package com.kaitohuy.chiabill.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivateFundRequest {
    private BigDecimal alertThreshold;
    private Long treasurerId;
}
