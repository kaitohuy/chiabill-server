package com.kaitohuy.chiabill.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
}