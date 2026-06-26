-- =============================================
-- V5: Create budgets table
-- =============================================
CREATE TABLE budgets (
    id            UUID PRIMARY KEY,
    account_id    UUID NOT NULL,
    category      VARCHAR(30) NOT NULL,
    monthly_limit NUMERIC(19, 2) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_budgets_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT uq_budgets_account_category UNIQUE (account_id, category)
);
