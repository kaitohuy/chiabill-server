-- Migration script to add EN columns for App Announcements and Places
-- V15__add_localization_columns.sql

-- 1. Add action_label_en to app_announcements
ALTER TABLE app_announcements ADD COLUMN action_label_en VARCHAR(100);

-- 2. Add EN columns to places
ALTER TABLE places ADD COLUMN name_en VARCHAR(255);
ALTER TABLE places ADD COLUMN summary_en TEXT;
ALTER TABLE places ADD COLUMN city_en VARCHAR(255);
ALTER TABLE places ADD COLUMN ticket_prices_en TEXT;
ALTER TABLE places ADD COLUMN opening_hours_en VARCHAR(100);
