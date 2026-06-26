-- =============================================
-- V2: Create accounts table
-- =============================================
CREATE TABLE accounts (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    currency     VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
