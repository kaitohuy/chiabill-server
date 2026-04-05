package com.kaitohuy.chiabill.dto.request;

import lombok.Data;

@Data
public class RegisterTokenRequest {
    private String token;
    private String platform;
}
