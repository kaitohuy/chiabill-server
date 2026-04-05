package com.kaitohuy.chiabill.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementResponse {

    private Long fromUserId;
    private String fromUserName;

    private Long toUserId;
    private String toUserName;

    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal paidAmount;

    private Boolean fromUserActive;
    private Boolean toUserActive;
}