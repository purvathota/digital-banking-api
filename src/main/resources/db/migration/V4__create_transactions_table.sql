-- =============================================
-- V4: Create transactions table
-- =============================================
CREATE TABLE transactions (
    id           UUID PRIMARY KEY,
    account_id   UUID NOT NULL,
    type         VARCHAR(30) NOT NULL,
    category     VARCHAR(30) NOT NULL,
    amount       NUMERIC(19, 2) NOT NULL,
    description  VARCHAR(500),
    reference_id UUID,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_transactions_account_created ON transactions(account_id, created_at);
