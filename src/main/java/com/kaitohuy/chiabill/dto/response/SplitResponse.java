package com.kaitohuy.chiabill.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SplitResponse {
    private Long userId;
    private String userName;
    private BigDecimal amount;
}