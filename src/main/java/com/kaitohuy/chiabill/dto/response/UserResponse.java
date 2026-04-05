package com.kaitohuy.chiabill.dto.response;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String avatarUrl;
    private String bankQrUrl;
    private Boolean isGhost;
    private String bankId;
    private String accountNo;
    private Integer paymentPriority;
    private String phone;
    private Boolean allowAutoAdd;
    private Boolean allowAutoApprovePayment;
}