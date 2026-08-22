ALTER TABLE app_users
    ADD COLUMN profile_image_key VARCHAR(500),
    ADD COLUMN profile_image_updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE app_users DROP CONSTRAINT chk_app_users_status;
ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_status
        CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED', 'DISABLED', 'BANNED'));

CREATE TABLE user_account_bans (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    banned_by UUID NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    banned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    unbanned_by UUID,
    unbanned_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_user_account_bans_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_user_account_bans_banned_by
        FOREIGN KEY (banned_by) REFERENCES app_users (id),
    CONSTRAINT fk_user_account_bans_unbanned_by
        FOREIGN KEY (unbanned_by) REFERENCES app_users (id),
    CONSTRAINT chk_user_account_bans_reason
        CHECK (CHAR_LENGTH(BTRIM(reason)) BETWEEN 3 AND 1000),
    CONSTRAINT chk_user_account_bans_previous_status
        CHECK (previous_status IN ('ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED', 'DISABLED')),
    CONSTRAINT chk_user_account_bans_unban_state CHECK (
        (unbanned_at IS NULL AND unbanned_by IS NULL)
        OR (unbanned_at IS NOT NULL AND unbanned_by IS NOT NULL AND unbanned_at >= banned_at)
    )
);

CREATE UNIQUE INDEX uk_user_account_bans_active
    ON user_account_bans (user_id)
    WHERE unbanned_at IS NULL;
CREATE INDEX idx_user_account_bans_user_time
    ON user_account_bans (user_id, banned_at DESC);
CREATE INDEX idx_user_account_bans_banned_by_time
    ON user_account_bans (banned_by, banned_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_user_id UUID,
    actor_role VARCHAR(32),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_audit_events_actor
        FOREIGN KEY (actor_user_id) REFERENCES app_users (id),
    CONSTRAINT chk_audit_events_actor_role
        CHECK (actor_role IS NULL OR actor_role IN ('PATIENT', 'DOCTOR', 'PHARMACIST', 'ADMIN')),
    CONSTRAINT chk_audit_events_action
        CHECK (CHAR_LENGTH(BTRIM(action)) BETWEEN 1 AND 100),
    CONSTRAINT chk_audit_events_target_type
        CHECK (target_type IS NULL OR CHAR_LENGTH(BTRIM(target_type)) BETWEEN 1 AND 100)
);

CREATE INDEX idx_audit_events_occurred_at
    ON audit_events (occurred_at DESC, id DESC);
CREATE INDEX idx_audit_events_actor_time
    ON audit_events (actor_user_id, occurred_at DESC);
CREATE INDEX idx_audit_events_action_time
    ON audit_events (action, occurred_at DESC);
CREATE INDEX idx_audit_events_target
    ON audit_events (target_type, target_id, occurred_at DESC);

ALTER TABLE consultation_messages
    ALTER COLUMN content DROP NOT NULL;
ALTER TABLE consultation_messages
    DROP CONSTRAINT chk_consultation_message_content;
ALTER TABLE consultation_messages
    ADD CONSTRAINT chk_consultation_message_content
        CHECK (content IS NULL OR CHAR_LENGTH(BTRIM(content)) BETWEEN 1 AND 4000);

CREATE TABLE consultation_message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255),
    byte_size BIGINT NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_consultation_message_attachments_message
        FOREIGN KEY (message_id) REFERENCES consultation_messages (id) ON DELETE CASCADE,
    CONSTRAINT uk_consultation_message_attachments_position
        UNIQUE (message_id, position),
    CONSTRAINT uk_consultation_message_attachments_storage_key
        UNIQUE (storage_key),
    CONSTRAINT chk_consultation_message_attachments_content_type
        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT chk_consultation_message_attachments_byte_size
        CHECK (byte_size BETWEEN 1 AND 5242880),
    CONSTRAINT chk_consultation_message_attachments_position
        CHECK (position BETWEEN 1 AND 4)
);

CREATE INDEX idx_consultation_message_attachments_message
    ON consultation_message_attachments (message_id, position);

ALTER TABLE prescriptions
    ADD COLUMN doctor_fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN doctor_fee_currency CHAR(3) NOT NULL DEFAULT 'LKR',
    ADD COLUMN doctor_fee_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN doctor_fee_confirmed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN doctor_fee_confirmed_by UUID,
    ADD CONSTRAINT fk_prescriptions_doctor_fee_confirmed_by
        FOREIGN KEY (doctor_fee_confirmed_by) REFERENCES app_users (id),
    ADD CONSTRAINT chk_prescriptions_doctor_fee_amount
        CHECK (doctor_fee_amount BETWEEN 0 AND 99999999.99),
    ADD CONSTRAINT chk_prescriptions_doctor_fee_currency
        CHECK (doctor_fee_currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT chk_prescriptions_doctor_fee_status
        CHECK (doctor_fee_status IN ('NOT_REQUIRED', 'AWAITING_CONFIRMATION', 'CONFIRMED')),
    ADD CONSTRAINT chk_prescriptions_doctor_fee_confirmation CHECK (
        (doctor_fee_status IN ('NOT_REQUIRED', 'AWAITING_CONFIRMATION')
            AND doctor_fee_confirmed_at IS NULL AND doctor_fee_confirmed_by IS NULL)
        OR (doctor_fee_status = 'CONFIRMED'
            AND doctor_fee_amount > 0
            AND doctor_fee_confirmed_at IS NOT NULL AND doctor_fee_confirmed_by IS NOT NULL)
    );

CREATE INDEX idx_prescriptions_doctor_fee_status
    ON prescriptions (doctor_fee_status, updated_at DESC);
