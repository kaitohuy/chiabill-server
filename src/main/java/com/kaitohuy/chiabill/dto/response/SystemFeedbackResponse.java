package com.kaitohuy.chiabill.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class SystemFeedbackResponse {
    private Long id;
    private UserResponse user;
    private String content;
    private LocalDateTime createdAt;
}
