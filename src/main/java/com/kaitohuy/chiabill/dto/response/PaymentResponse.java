package com.kaitohuy.chiabill.dto.response;

import com.kaitohuy.chiabill.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long tripId;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private BigDecimal amount;
    private String proofUrl;
    private PaymentStatus status;
    private LocalDateTime createdAt;
}
