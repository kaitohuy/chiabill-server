package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "idToken cannot be blank")
    @Size(max = 4000, message = "idToken must not exceed 4000 characters")
    private String idToken;
}