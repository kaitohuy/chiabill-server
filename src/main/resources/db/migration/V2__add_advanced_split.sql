-- Thêm cột split_type vào bảng expenses (mặc định là 'EQUAL')
ALTER TABLE expenses ADD COLUMN split_type VARCHAR(20) DEFAULT 'EQUAL' NOT NULL;

-- Thêm cột split_value vào bảng expense_splits (để lưu trữ giá trị gốc: phần trăm, tỷ trọng, hoặc null nếu chia đều/số tiền cụ thể)
ALTER TABLE expense_splits ADD COLUMN split_value NUMERIC(15, 2);
