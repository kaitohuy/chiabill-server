ALTER TABLE group_fund_contributions ADD COLUMN linked_expense_id BIGINT NULL;
ALTER TABLE group_fund_contributions ADD CONSTRAINT fk_gfc_linked_expense FOREIGN KEY (linked_expense_id) REFERENCES expenses(id);
