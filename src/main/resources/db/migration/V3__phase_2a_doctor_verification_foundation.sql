CREATE TABLE hospitals (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address_line VARCHAR(255),
    city VARCHAR(100),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_hospitals_name_ci ON hospitals (LOWER(name));
CREATE INDEX idx_hospitals_active_name ON hospitals (active, name);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_departments_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals (id)
);

CREATE UNIQUE INDEX uk_departments_hospital_name_ci
    ON departments (hospital_id, LOWER(name));
CREATE INDEX idx_departments_hospital_active_name
    ON departments (hospital_id, active, name);

CREATE TABLE specializations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_specializations_name_ci ON specializations (LOWER(name));
CREATE INDEX idx_specializations_active_name ON specializations (active, name);

ALTER TABLE doctor_profiles
    ADD COLUMN hospital_id UUID,
    ADD COLUMN department_id UUID,
    ADD COLUMN specialization_id UUID,
    ADD COLUMN qualifications VARCHAR(500),
    ADD COLUMN years_of_experience INTEGER,
    ADD COLUMN bio VARCHAR(2000),
    ADD COLUMN verification_rejection_reason VARCHAR(1000),
    ADD COLUMN verified_by UUID,
    ADD COLUMN verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN submitted_for_verification_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT fk_doctor_profiles_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    ADD CONSTRAINT fk_doctor_profiles_department
        FOREIGN KEY (department_id) REFERENCES departments (id),
    ADD CONSTRAINT fk_doctor_profiles_specialization
        FOREIGN KEY (specialization_id) REFERENCES specializations (id),
    ADD CONSTRAINT fk_doctor_profiles_verified_by
        FOREIGN KEY (verified_by) REFERENCES app_users (id),
    ADD CONSTRAINT chk_doctor_years_of_experience
        CHECK (years_of_experience IS NULL OR years_of_experience >= 0);

CREATE UNIQUE INDEX uk_doctor_profiles_registration_ci
    ON doctor_profiles (LOWER(medical_registration_number))
    WHERE medical_registration_number IS NOT NULL;
CREATE INDEX idx_doctor_profiles_verification_queue
    ON doctor_profiles (verification_status, submitted_for_verification_at);

