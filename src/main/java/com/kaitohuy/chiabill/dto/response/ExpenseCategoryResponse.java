package com.kaitohuy.chiabill.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseCategoryResponse {
    private Long id;
    private String name;
    private String icon;
    private Long tripId;
}
