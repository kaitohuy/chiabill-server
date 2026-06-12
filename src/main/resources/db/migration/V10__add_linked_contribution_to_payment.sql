ALTER TABLE payments ADD COLUMN linked_contribution_id BIGINT NULL;
ALTER TABLE payments ADD CONSTRAINT fk_payment_linked_contribution FOREIGN KEY (linked_contribution_id) REFERENCES group_fund_contributions(id);
