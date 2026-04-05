package com.kaitohuy.chiabill.dto.request;

import lombok.Data;

@Data
public class CreateTripRequest {
    private String name;
    private String description;
}