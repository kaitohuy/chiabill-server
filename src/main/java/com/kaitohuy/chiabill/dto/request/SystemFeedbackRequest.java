package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemFeedbackRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String content;
}
