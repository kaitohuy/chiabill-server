package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_announcements", indexes = {
    @Index(name = "idx_announcement_type", columnList = "type"),
    @Index(name = "idx_announcement_active", columnList = "is_active"),
    @Index(name = "idx_announcement_platform", columnList = "platform"),
    @Index(name = "idx_announcement_end_at", columnList = "end_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAnnouncement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== PHÂN LOẠI =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementType type;

    // ===== NỘI DUNG =====
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** Banner image hoặc ảnh minh họa chính */
    @Column(length = 500)
    private String imageUrl;

    // ===== HÀNH ĐỘNG KHI CLICK =====
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ActionType actionType = ActionType.NONE;

    /** URL web hoặc deep link (vd: app://screen/payment, https://...) */
    @Column(length = 500)
    private String actionUrl;

    /** Text hiển thị trên nút bấm (vd: "Cập nhật ngay", "Chuyển khoản", "Xem thêm") */
    @Column(length = 100)
    private String actionLabel;

    // ===== CHỈ DÙNG CHO UPDATE =====
    /** Version code tối thiểu, dưới mức này sẽ bị yêu cầu update */
    private Integer minVersion;

    /** Version code mới nhất trên store */
    private Integer latestVersion;

    /** TRUE = không cho dùng app nếu chưa update */
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isForceUpdate = false;

    // ===== CHỈ DÙNG CHO PAYMENT / DONATE =====
    /** Ảnh QR ngân hàng để người dùng quét */
    @Column(length = 500)
    private String qrImageUrl;

    /**
     * Thông tin ngân hàng dạng JSON
     * VD: {"bank":"VCB","account":"1234567890","name":"NGUYEN VAN A","branch":"HN"}
     */
    @Column(columnDefinition = "JSON")
    private String bankInfo;

    /** Số tiền gợi ý (đơn vị: VND) */
    @Column(precision = 15, scale = 2)
    private BigDecimal suggestedAmount;

    // ===== ĐIỀU KIỆN HIỂN THỊ =====
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Platform platform = Platform.ALL;

    /** Số càng cao càng được hiển thị trước */
    @Column(columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer priority = 0;

    /** TRUE = người dùng có thể bấm "Bỏ qua / Đóng" */
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isDismissible = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    @Builder.Default
    private DisplayMode displayMode = DisplayMode.ONCE;

    // ===== THỜI GIAN SỐNG =====
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isActive = true;

    /** NULL = hiện ngay khi được tạo */
    private LocalDateTime startAt;

    /** NULL = không hết hạn */
    private LocalDateTime endAt;

    /** Admin đã tạo thông báo này */
    private Long createdBy;

    // ===== ENUMS =====
    public enum AnnouncementType {
        UPDATE,         // Cập nhật app
        ANNOUNCEMENT,   // Thông báo chung từ admin
        PAYMENT,        // Thu phí / gia hạn subscription
        DONATE,         // Quyên góp chi phí server
        PROMOTION,      // Tính năng mới / khuyến mãi
        MAINTENANCE     // Bảo trì / tạm ngưng dịch vụ
    }

    public enum ActionType {
        NONE,           // Chỉ đọc, không có hành động
        OPEN_URL,       // Mở trình duyệt
        OPEN_STORE,     // Mở CH Play / App Store
        OPEN_SCREEN,    // Deep link vào màn hình trong app
        DISMISS         // Chỉ đóng dialog
    }

    public enum Platform {
        ALL,
        ANDROID,
        IOS
    }

    public enum DisplayMode {
        ONCE,           // Chỉ hiện 1 lần (lưu trạng thái ở client)
        EVERY_LAUNCH,   // Hiện mỗi lần mở app
        DAILY,          // Hiện 1 lần mỗi ngày
        ALWAYS          // Luôn hiện cho đến khi hết hạn
    }
}
