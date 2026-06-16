package com.kaitohuy.chiabill.dto.response;

import com.kaitohuy.chiabill.entity.AppAnnouncement;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AppAnnouncementResponse {

    private Long id;
    private AppAnnouncement.AnnouncementType type;

    // Nội dung
    private String title;
    private String content;
    private String imageUrl;

    // Hành động
    private AppAnnouncement.ActionType actionType;
    private String actionUrl;
    private String actionLabel;

    // Chỉ cho UPDATE
    private Integer minVersion;
    private Integer latestVersion;
    private Boolean isForceUpdate;

    // Chỉ cho PAYMENT / DONATE
    private String qrImageUrl;
    private String bankInfo;
    private BigDecimal suggestedAmount;

    // Hiển thị
    private AppAnnouncement.Platform platform;
    private Integer priority;
    private Boolean isDismissible;
    private AppAnnouncement.DisplayMode displayMode;

    // Thời gian
    private Boolean isActive;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
}
