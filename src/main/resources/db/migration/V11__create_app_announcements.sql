-- ================================================================
-- V11: Tạo bảng app_announcements
-- Mục đích: Hệ thống thông báo từ admin tới client
-- Bao gồm: update app, thông báo chung, thu phí, donate, promotion, bảo trì
-- ================================================================

CREATE TABLE IF NOT EXISTS app_announcements (
    id              BIGINT          NOT NULL AUTO_INCREMENT,

    -- Phân loại
    type            VARCHAR(20)     NOT NULL,
    CONSTRAINT chk_announcement_type CHECK (
        type IN ('UPDATE', 'ANNOUNCEMENT', 'PAYMENT', 'DONATE', 'PROMOTION', 'MAINTENANCE')
    ),

    -- Nội dung
    title           VARCHAR(255)    NOT NULL,
    content         TEXT,
    image_url       VARCHAR(500),

    -- Hành động khi click
    action_type     VARCHAR(20)     NOT NULL DEFAULT 'NONE',
    CONSTRAINT chk_action_type CHECK (
        action_type IN ('NONE', 'OPEN_URL', 'OPEN_STORE', 'OPEN_SCREEN', 'DISMISS')
    ),
    action_url      VARCHAR(500),
    action_label    VARCHAR(100),

    -- Chỉ dùng cho type = UPDATE
    min_version     INT,
    latest_version  INT,
    is_force_update BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Chỉ dùng cho type = PAYMENT / DONATE
    qr_image_url    VARCHAR(500),
    bank_info       JSON,
    suggested_amount DECIMAL(15, 2),

    -- Điều kiện hiển thị
    platform        VARCHAR(10)     NOT NULL DEFAULT 'ALL',
    CONSTRAINT chk_platform CHECK (
        platform IN ('ALL', 'ANDROID', 'IOS')
    ),
    priority        INT             NOT NULL DEFAULT 0,
    is_dismissible  BOOLEAN         NOT NULL DEFAULT TRUE,
    display_mode    VARCHAR(15)     NOT NULL DEFAULT 'ONCE',
    CONSTRAINT chk_display_mode CHECK (
        display_mode IN ('ONCE', 'EVERY_LAUNCH', 'DAILY', 'ALWAYS')
    ),

    -- Thời gian sống
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    start_at        DATETIME,
    end_at          DATETIME,

    -- Audit (ai tạo)
    created_by      BIGINT,

    -- BaseEntity fields
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id)
);

-- Indexes để query nhanh khi client gọi GET /active
CREATE INDEX idx_announcement_type     ON app_announcements (type);
CREATE INDEX idx_announcement_active   ON app_announcements (is_active);
CREATE INDEX idx_announcement_platform ON app_announcements (platform);
CREATE INDEX idx_announcement_end_at   ON app_announcements (end_at);
CREATE INDEX idx_announcement_priority ON app_announcements (priority DESC);

-- ================================================================
-- Seed data mẫu: 1 thông báo chào mừng hiện 1 lần khi vào app
-- (Xóa dòng này nếu không cần data mẫu)
-- ================================================================
INSERT INTO app_announcements (
    type, title, content,
    action_type, action_label,
    platform, priority, is_dismissible, display_mode,
    is_active
) VALUES (
    'ANNOUNCEMENT',
    'Chào mừng đến với DuliVie!',
    'Cảm ơn bạn đã tin dùng ứng dụng. Hãy tạo chuyến đi đầu tiên và trải nghiệm các tính năng nhé!',
    'DISMISS', 'Bắt đầu ngay',
    'ALL', 1, TRUE, 'ONCE',
    TRUE
);
