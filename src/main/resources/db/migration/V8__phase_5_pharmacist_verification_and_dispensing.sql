ALTER TABLE pharmacist_profiles
    ADD COLUMN pharmacy_registration_number VARCHAR(100),
    ADD COLUMN pharmacy_address VARCHAR(500),
    ADD COLUMN qualifications VARCHAR(500),
    ADD COLUMN verification_rejection_reason VARCHAR(1000),
    ADD COLUMN verified_by UUID,
    ADD COLUMN verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN submitted_for_verification_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT fk_pharmacist_profiles_verified_by
        FOREIGN KEY (verified_by) REFERENCES app_users (id);

CREATE UNIQUE INDEX uk_pharmacist_profiles_registration_ci
    ON pharmacist_profiles (LOWER(professional_registration_number))
    WHERE professional_registration_number IS NOT NULL;

CREATE INDEX idx_pharmacist_profiles_verification_queue
    ON pharmacist_profiles (verification_status, submitted_for_verification_at);

CREATE TABLE prescription_dispensations (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL,
    pharmacist_id UUID NOT NULL,
    dispensed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    pharmacy_name_snapshot VARCHAR(200) NOT NULL,
    pharmacist_registration_snapshot VARCHAR(100) NOT NULL,
    dispensing_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_prescription_dispensations_prescription UNIQUE (prescription_id),
    CONSTRAINT fk_prescription_dispensations_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT fk_prescription_dispensations_pharmacist
        FOREIGN KEY (pharmacist_id) REFERENCES pharmacist_profiles (id),
    CONSTRAINT chk_prescription_dispensations_pharmacy_name
        CHECK (CHAR_LENGTH(BTRIM(pharmacy_name_snapshot)) BETWEEN 1 AND 200),
    CONSTRAINT chk_prescription_dispensations_registration
        CHECK (CHAR_LENGTH(BTRIM(pharmacist_registration_snapshot)) BETWEEN 1 AND 100),
    CONSTRAINT chk_prescription_dispensations_note
        CHECK (dispensing_note IS NULL OR CHAR_LENGTH(dispensing_note) <= 1000)
);

CREATE INDEX idx_prescription_dispensations_pharmacist_time
    ON prescription_dispensations (pharmacist_id, dispensed_at DESC);
