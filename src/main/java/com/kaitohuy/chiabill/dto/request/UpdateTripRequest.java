package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTripRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;
}
