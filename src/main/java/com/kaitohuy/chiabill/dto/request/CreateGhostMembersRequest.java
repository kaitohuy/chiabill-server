package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateGhostMembersRequest {

    @NotEmpty(message = "Names list cannot be empty")
    @Size(max = 50, message = "Cannot add more than 50 ghost members at once")
    private List<String> names;
}
