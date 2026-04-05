package com.kaitohuy.chiabill.dto.response;

import lombok.Data;

@Data
public class GhostMemberResponse {
    private Long id;
    private String name;
    private Boolean isGhost;
    private Long managedById;
}
