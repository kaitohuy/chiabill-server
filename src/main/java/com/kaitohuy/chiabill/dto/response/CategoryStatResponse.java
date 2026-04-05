package com.kaitohuy.chiabill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryStatResponse {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private BigDecimal totalAmount;
}
