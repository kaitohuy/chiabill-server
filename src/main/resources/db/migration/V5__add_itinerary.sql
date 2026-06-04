-- Thêm trường end_date vào bảng trips
ALTER TABLE trips ADD COLUMN end_date TIMESTAMP WITHOUT TIME ZONE;

-- Tạo bảng itinerary_items
CREATE TABLE itinerary_items (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    day_number INT NOT NULL,
    time_range VARCHAR(50),
    activity TEXT NOT NULL,
    location VARCHAR(255),
    note TEXT,
    estimated_cost DECIMAL(15, 2),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_itinerary_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- Tạo index cho trip_id trong itinerary_items
CREATE INDEX idx_itinerary_trip ON itinerary_items(trip_id);
