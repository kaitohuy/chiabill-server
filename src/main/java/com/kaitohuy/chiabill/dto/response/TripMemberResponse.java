package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TripMemberResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private String role;
    private String status;
}
