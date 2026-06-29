package com.kaitohuy.chiabill.dto.request;

import com.kaitohuy.chiabill.entity.AppAnnouncement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AppAnnouncementRequest {

    @NotNull(message = "Loại thông báo không được để trống")
    private AppAnnouncement.AnnouncementType type;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 255, message = "Tiêu đề tiếng Anh tối đa 255 ký tự")
    private String titleEn;

    private String content;
    private String contentEn;
    private String imageUrl;

    private AppAnnouncement.ActionType actionType;
    private String actionUrl;

    @Size(max = 100)
    private String actionLabel;

    @Size(max = 100)
    private String actionLabelEn;

    // Cho UPDATE
    private Integer minVersion;
    private Integer latestVersion;
    private Boolean isForceUpdate;

    // Cho PAYMENT / DONATE
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
}
