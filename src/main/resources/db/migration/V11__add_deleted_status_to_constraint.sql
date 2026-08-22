-- V11__add_deleted_status_to_constraint.sql
-- Add DELETED to the allowed account status values

ALTER TABLE app_users DROP CONSTRAINT chk_app_users_status;
ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_status
        CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED', 'DISABLED', 'BANNED', 'DELETED'));
