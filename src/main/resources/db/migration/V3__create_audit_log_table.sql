-- =============================================
-- V3: Create audit_log table (APPEND-ONLY)
-- =============================================
-- This table is designed as an immutable, append-only ledger.
-- No rows should ever be updated or deleted. A trigger enforces this.
-- This ensures a complete, tamper-evident history of every financial
-- operation, enabling reconstruction of account state at any point in time.

CREATE TABLE audit_log (
    id               BIGSERIAL PRIMARY KEY,
    account_id       UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount           NUMERIC(19, 2) NOT NULL,
    balance_before   NUMERIC(19, 2) NOT NULL,
    balance_after    NUMERIC(19, 2) NOT NULL,
    description      VARCHAR(500),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);

COMMENT ON TABLE audit_log IS
    'APPEND-ONLY audit ledger. UPDATE and DELETE operations are blocked by a database trigger. '
    'This ensures an immutable, tamper-evident record of every financial transaction for regulatory compliance.';

-- Trigger function: raises an exception on any UPDATE or DELETE attempt
CREATE OR REPLACE FUNCTION prevent_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log table is append-only. UPDATE and DELETE operations are prohibited.';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger: fires BEFORE UPDATE or DELETE on audit_log
CREATE TRIGGER trg_audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_modification();

CREATE INDEX idx_audit_log_account_id ON audit_log(account_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
