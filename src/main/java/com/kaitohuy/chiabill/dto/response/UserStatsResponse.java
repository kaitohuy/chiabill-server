package com.kaitohuy.chiabill.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers; // Non-anonymous & Non-ghost users
    private long anonymousUsers;
    private long ghostUsers;
}
