-- Tạo bảng itinerary_alarm_settings
CREATE TABLE IF NOT EXISTS itinerary_alarm_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    alarm_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    alarm_value INT NOT NULL DEFAULT 15,
    alarm_unit VARCHAR(20) NOT NULL DEFAULT 'Phút',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_alarm_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_alarm_settings_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_trip UNIQUE (user_id, trip_id)
);

-- Tạo bảng scheduled_notifications
CREATE TABLE IF NOT EXISTS scheduled_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    itinerary_item_id BIGINT,
    scheduled_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_scheduled_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_scheduled_notification_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_scheduled_notification_item FOREIGN KEY (itinerary_item_id) REFERENCES itinerary_items(id) ON DELETE SET NULL
);

-- Tạo index tối ưu cho scheduler quét
CREATE INDEX IF NOT EXISTS idx_scheduled_time_sent ON scheduled_notifications(scheduled_time, is_sent);

-- Cập nhật check constraint của bảng notifications để hỗ trợ loại thông báo 'ITINERARY'
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check 
    CHECK (type::text = ANY (ARRAY[
        'EXPENSE_CREATED'::text, 
        'PAYMENT_REQUESTED'::text, 
        'PAYMENT_APPROVED'::text, 
        'TRIP_INVITE'::text, 
        'MEMBER_KICKED'::text, 
        'SYSTEM'::text, 
        'SETTLEMENT_CREATED'::text, 
        'ITINERARY'::text
    ]));
