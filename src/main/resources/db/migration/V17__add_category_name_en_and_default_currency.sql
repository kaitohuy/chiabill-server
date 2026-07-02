-- Add default_currency to users
ALTER TABLE users ADD COLUMN default_currency VARCHAR(10) DEFAULT 'VND';
UPDATE users SET default_currency = 'VND' WHERE default_currency IS NULL;

-- Add name_en to expense_categories
ALTER TABLE expense_categories ADD COLUMN name_en VARCHAR(255);

-- Translate existing system categories
UPDATE expense_categories SET name_en = 'Food & Dining' WHERE name = 'Ăn uống (Chung)';
UPDATE expense_categories SET name_en = 'Coffee & Drinks' WHERE name = 'Cà phê & Nước';
UPDATE expense_categories SET name_en = 'Drinks & Party' WHERE name = 'Nhậu & Party';
UPDATE expense_categories SET name_en = 'Street Food / Snacks' WHERE name = 'Ăn vặt / Lề đường';
UPDATE expense_categories SET name_en = 'Groceries' WHERE name = 'Đi chợ / Siêu thị';

UPDATE expense_categories SET name_en = 'Flights / Trains' WHERE name = 'Vé máy bay / Tàu';
UPDATE expense_categories SET name_en = 'Bus / Coach' WHERE name = 'Xe khách / Bus';
UPDATE expense_categories SET name_en = 'Taxi / Grab' WHERE name = 'Taxi / Grab';
UPDATE expense_categories SET name_en = 'Vehicle Rental' WHERE name = 'Thuê xe';
UPDATE expense_categories SET name_en = 'Gas / Parking' WHERE name = 'Đổ xăng / Gửi xe';
UPDATE expense_categories SET name_en = 'Tolls' WHERE name = 'Phí cầu đường / Trạm thu phí';

UPDATE expense_categories SET name_en = 'Hotel / Homestay' WHERE name = 'Khách sạn / Homestay';
UPDATE expense_categories SET name_en = 'Camping / Tent' WHERE name = 'Thuê lều / Cắm trại';

UPDATE expense_categories SET name_en = 'Sightseeing Tickets' WHERE name = 'Vé tham quan';
UPDATE expense_categories SET name_en = 'Tour / Guide' WHERE name = 'Tour du lịch';
UPDATE expense_categories SET name_en = 'Karaoke / Club' WHERE name = 'Karaoke / Club';
UPDATE expense_categories SET name_en = 'Team Building' WHERE name = 'Team Building';

UPDATE expense_categories SET name_en = 'Shopping / Gifts' WHERE name = 'Mua sắm / Quà cáp';
UPDATE expense_categories SET name_en = 'Medical / Pharmacy' WHERE name = 'Y tế / Thuốc men';
UPDATE expense_categories SET name_en = 'Personal Care' WHERE name = 'Đồ dùng cá nhân';
UPDATE expense_categories SET name_en = 'Tips' WHERE name = 'Tiền Tip / Bồi dưỡng';
UPDATE expense_categories SET name_en = 'Incidental Expenses' WHERE name = 'Chi phí phát sinh';
UPDATE expense_categories SET name_en = 'Group Fund' WHERE name = 'Quỹ chung';
