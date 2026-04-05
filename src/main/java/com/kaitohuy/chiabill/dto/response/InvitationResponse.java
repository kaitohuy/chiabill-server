package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvitationResponse {
    private String inviteCode;
    private String inviteLink;
    private Long tripId;
    private String tripName;
    private Integer memberCount;
    private LocalDateTime expiresAt;
}
