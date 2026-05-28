package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceCommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;
}
