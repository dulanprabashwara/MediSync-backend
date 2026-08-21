CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    consultation_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    validity_days INTEGER NOT NULL,
    general_instructions TEXT,
    issued_at TIMESTAMP WITH TIME ZONE,
    valid_until TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_prescriptions_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation_sessions (id),
    CONSTRAINT fk_prescriptions_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id),
    CONSTRAINT fk_prescriptions_patient
        FOREIGN KEY (patient_id) REFERENCES patient_profiles (id),
    CONSTRAINT chk_prescriptions_status
        CHECK (status IN ('DRAFT', 'ISSUED', 'CANCELLED')),
    CONSTRAINT chk_prescriptions_validity_days
        CHECK (validity_days BETWEEN 1 AND 90),
    CONSTRAINT chk_prescriptions_lifecycle CHECK (
        (status = 'DRAFT'
            AND issued_at IS NULL AND valid_until IS NULL
            AND cancelled_at IS NULL AND cancellation_reason IS NULL)
        OR (status = 'ISSUED'
            AND issued_at IS NOT NULL AND valid_until IS NOT NULL
            AND cancelled_at IS NULL AND cancellation_reason IS NULL)
        OR (status = 'CANCELLED'
            AND issued_at IS NOT NULL AND valid_until IS NOT NULL
            AND cancelled_at IS NOT NULL
            AND CHAR_LENGTH(BTRIM(cancellation_reason)) BETWEEN 3 AND 1000)
    ),
    CONSTRAINT chk_prescriptions_valid_until
        CHECK (issued_at IS NULL OR valid_until > issued_at),
    CONSTRAINT chk_prescriptions_general_instructions
        CHECK (general_instructions IS NULL OR CHAR_LENGTH(general_instructions) <= 5000)
);

CREATE UNIQUE INDEX uk_prescriptions_consultation_draft
    ON prescriptions (consultation_id)
    WHERE status = 'DRAFT';
CREATE INDEX idx_prescriptions_patient_issued
    ON prescriptions (patient_id, issued_at DESC);
CREATE INDEX idx_prescriptions_doctor_issued
    ON prescriptions (doctor_id, issued_at DESC);
CREATE INDEX idx_prescriptions_consultation_created
    ON prescriptions (consultation_id, created_at DESC);
CREATE INDEX idx_prescriptions_status
    ON prescriptions (status);

CREATE TABLE prescription_items (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL,
    position INTEGER NOT NULL,
    medicine_name VARCHAR(200) NOT NULL,
    strength VARCHAR(100),
    dosage VARCHAR(200) NOT NULL,
    frequency VARCHAR(200) NOT NULL,
    duration VARCHAR(200) NOT NULL,
    quantity VARCHAR(100),
    medicine_form VARCHAR(100),
    route VARCHAR(100),
    instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_prescription_items_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT uk_prescription_items_position
        UNIQUE (prescription_id, position),
    CONSTRAINT chk_prescription_items_position
        CHECK (position > 0),
    CONSTRAINT chk_prescription_items_medicine
        CHECK (CHAR_LENGTH(BTRIM(medicine_name)) BETWEEN 1 AND 200),
    CONSTRAINT chk_prescription_items_dosage
        CHECK (CHAR_LENGTH(BTRIM(dosage)) BETWEEN 1 AND 200),
    CONSTRAINT chk_prescription_items_frequency
        CHECK (CHAR_LENGTH(BTRIM(frequency)) BETWEEN 1 AND 200),
    CONSTRAINT chk_prescription_items_duration
        CHECK (CHAR_LENGTH(BTRIM(duration)) BETWEEN 1 AND 200),
    CONSTRAINT chk_prescription_items_instructions
        CHECK (instructions IS NULL OR CHAR_LENGTH(instructions) <= 2000)
);

CREATE INDEX idx_prescription_items_prescription_position
    ON prescription_items (prescription_id, position);

CREATE TABLE prescription_qr_tokens (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL,
    token VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_prescription_qr_tokens_prescription UNIQUE (prescription_id),
    CONSTRAINT uk_prescription_qr_tokens_token UNIQUE (token),
    CONSTRAINT fk_prescription_qr_tokens_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT chk_prescription_qr_tokens_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_prescription_qr_tokens_revocation
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_prescription_qr_tokens_token
    ON prescription_qr_tokens (token);
