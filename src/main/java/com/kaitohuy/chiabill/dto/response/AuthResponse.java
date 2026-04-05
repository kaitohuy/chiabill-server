package com.kaitohuy.chiabill.dto.response;

import lombok.*;

@Data
@Builder
public class AuthResponse {

    private String token;
    private UserResponse user;
}