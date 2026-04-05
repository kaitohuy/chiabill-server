package com.kaitohuy.chiabill.dto.response;

import com.kaitohuy.chiabill.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
