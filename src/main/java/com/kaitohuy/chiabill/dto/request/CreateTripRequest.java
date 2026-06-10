package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateTripRequest {
    @NotBlank(message = "Tên chuyến đi không được để trống")
    @Size(max = 100, message = "Tên chuyến đi không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @Size(max = 500, message = "Đường dẫn ảnh bìa không được vượt quá 500 ký tự")
    private String coverUrl;

    private BigDecimal totalBudget;
    private java.time.LocalDateTime startDate;
    private java.time.LocalDateTime endDate;

    @Size(max = 50, message = "Tên danh mục không được vượt quá 50 ký tự")
    private String categoryName;

    @Size(max = 50, message = "Icon danh mục không được vượt quá 50 ký tự")
    private String categoryIcon;
}