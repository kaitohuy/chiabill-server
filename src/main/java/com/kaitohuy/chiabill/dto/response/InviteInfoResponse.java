package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InviteInfoResponse {
    private String tripName;
    private String description;
    private Integer memberCount;
    private String createdByName;
    private LocalDateTime expiresAt;
}
