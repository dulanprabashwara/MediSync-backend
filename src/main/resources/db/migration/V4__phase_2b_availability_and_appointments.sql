CREATE TABLE doctor_availability_windows (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_availability_windows_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id),
    CONSTRAINT chk_availability_window_times CHECK (starts_at < ends_at),
    CONSTRAINT chk_availability_slot_duration
        CHECK (slot_duration_minutes IN (15, 20, 30, 45, 60))
);

CREATE INDEX idx_availability_windows_doctor_time
    ON doctor_availability_windows (doctor_id, starts_at, ends_at);
CREATE INDEX idx_availability_windows_active_time
    ON doctor_availability_windows (active, starts_at);

CREATE TABLE appointment_slots (
    id UUID PRIMARY KEY,
    availability_window_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_appointment_slots_window
        FOREIGN KEY (availability_window_id) REFERENCES doctor_availability_windows (id),
    CONSTRAINT fk_appointment_slots_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id),
    CONSTRAINT uk_appointment_slots_doctor_start UNIQUE (doctor_id, starts_at),
    CONSTRAINT chk_appointment_slot_times CHECK (starts_at < ends_at),
    CONSTRAINT chk_appointment_slot_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'BOOKED', 'BLOCKED'))
);

CREATE INDEX idx_appointment_slots_doctor_time
    ON appointment_slots (doctor_id, starts_at);
CREATE INDEX idx_appointment_slots_window_status
    ON appointment_slots (availability_window_id, status);
CREATE INDEX idx_appointment_slots_available_time
    ON appointment_slots (doctor_id, starts_at)
    WHERE status = 'AVAILABLE';

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_end TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    doctor_rejection_reason VARCHAR(1000),
    cancellation_reason VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id) REFERENCES patient_profiles (id),
    CONSTRAINT fk_appointments_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id),
    CONSTRAINT fk_appointments_slot
        FOREIGN KEY (slot_id) REFERENCES appointment_slots (id),
    CONSTRAINT chk_appointment_times CHECK (scheduled_start < scheduled_end),
    CONSTRAINT chk_appointment_status CHECK (status IN (
        'REQUESTED', 'CONFIRMED', 'REJECTED',
        'CANCELLED_BY_PATIENT', 'CANCELLED_BY_DOCTOR'
    ))
);

CREATE UNIQUE INDEX uk_appointments_active_slot
    ON appointments (slot_id)
    WHERE status IN ('REQUESTED', 'CONFIRMED');
CREATE INDEX idx_appointments_patient_time
    ON appointments (patient_id, scheduled_start);
CREATE INDEX idx_appointments_doctor_time
    ON appointments (doctor_id, scheduled_start);
CREATE INDEX idx_appointments_doctor_status_time
    ON appointments (doctor_id, status, scheduled_start);
CREATE INDEX idx_appointments_patient_active_time
    ON appointments (patient_id, scheduled_start, scheduled_end)
    WHERE status IN ('REQUESTED', 'CONFIRMED');

CREATE TABLE appointment_symptoms (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    reason_for_visit VARCHAR(300) NOT NULL,
    symptoms VARCHAR(2000) NOT NULL,
    symptom_duration VARCHAR(200),
    additional_notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_appointment_symptoms_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_appointment_symptoms_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id)
);
