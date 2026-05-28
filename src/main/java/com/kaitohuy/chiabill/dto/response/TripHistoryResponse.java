package com.kaitohuy.chiabill.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripHistoryResponse {
    private Long id;
    private Long tripId;
    private Long actorId;
    private String actorName;
    private String action;
    private String content;
    private LocalDateTime createdAt;
}
