package com.kaitohuy.chiabill.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BatchPayOnBehalfRequest {
    private Long toUserId;                  // Chủ nợ (người nhận tiền)
    private BigDecimal totalAmount;         // Tổng tiền gộp
    private List<Long> onBehalfOfUserIds;   // Danh sách người được trả hộ
    private List<BigDecimal> onBehalfOfAmounts; // Số tiền tương ứng của từng người
}
