-- V10__account_deletion_and_anonymization.sql

ALTER TABLE app_users
ADD COLUMN deleted_at TIMESTAMPTZ,
ADD COLUMN deleted_by_user_id UUID,
ADD COLUMN deletion_reason TEXT,
ADD COLUMN deletion_source VARCHAR(50);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_app_users_status ON app_users(status);
