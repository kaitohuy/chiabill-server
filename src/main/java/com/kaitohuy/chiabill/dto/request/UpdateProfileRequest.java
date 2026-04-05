package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100)
    @NotBlank(message = "Tên hiển thị không được để trống")
    private String name;

    private String bankId;

    private String accountNo;

    private String avatarUrl;

    private String bankQrUrl;

    private Integer paymentPriority;

    private String phone;

    private Boolean allowAutoAdd;
    private Boolean allowAutoApprovePayment;
}