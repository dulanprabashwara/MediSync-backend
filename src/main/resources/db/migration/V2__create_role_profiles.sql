CREATE TABLE patient_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_patient_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_patient_profiles_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE TABLE doctor_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    medical_registration_number VARCHAR(100),
    specialization VARCHAR(150),
    verification_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_doctor_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_doctor_profiles_registration UNIQUE (medical_registration_number),
    CONSTRAINT fk_doctor_profiles_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_doctor_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE pharmacist_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    professional_registration_number VARCHAR(100),
    pharmacy_name VARCHAR(200),
    verification_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_pharmacist_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_pharmacist_profiles_registration UNIQUE (professional_registration_number),
    CONSTRAINT fk_pharmacist_profiles_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_pharmacist_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

