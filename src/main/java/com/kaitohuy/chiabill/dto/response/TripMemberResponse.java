package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TripMemberResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private String role;
    private String status;
    private Boolean isGhost;
    private String bankId;
    private String accountNo;
    private Integer paymentPriority;
    private String bankQrUrl;
    private String email;
    private String phone;
}
