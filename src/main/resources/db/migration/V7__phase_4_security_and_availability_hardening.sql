UPDATE prescription_qr_tokens
SET revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP);

DROP INDEX IF EXISTS idx_prescription_qr_tokens_token;

ALTER TABLE prescription_qr_tokens
    DROP CONSTRAINT IF EXISTS uk_prescription_qr_tokens_token;

ALTER TABLE prescription_qr_tokens
    ADD COLUMN token_hash CHAR(64);

ALTER TABLE prescription_qr_tokens
    DROP COLUMN token;

ALTER TABLE prescription_qr_tokens
    ADD CONSTRAINT chk_prescription_qr_tokens_hash
        CHECK (token_hash IS NULL OR token_hash ~ '^[0-9a-f]{64}$');

CREATE UNIQUE INDEX uk_prescription_qr_tokens_token_hash
    ON prescription_qr_tokens (token_hash)
    WHERE token_hash IS NOT NULL;
