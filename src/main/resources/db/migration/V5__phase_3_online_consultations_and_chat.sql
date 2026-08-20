CREATE TABLE consultation_sessions (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_consultation_sessions_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_consultation_sessions_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT chk_consultation_session_status
        CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_consultation_session_timestamps CHECK (
        (status = 'SCHEDULED' AND started_at IS NULL AND completed_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'IN_PROGRESS' AND started_at IS NOT NULL AND completed_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND completed_at IS NULL AND cancelled_at IS NOT NULL)
    )
);

CREATE INDEX idx_consultation_sessions_status
    ON consultation_sessions (status);

CREATE TABLE consultation_messages (
    id UUID PRIMARY KEY,
    consultation_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_consultation_messages_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation_sessions (id),
    CONSTRAINT fk_consultation_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES app_users (id),
    CONSTRAINT chk_consultation_message_content
        CHECK (CHAR_LENGTH(BTRIM(content)) BETWEEN 1 AND 4000)
);

CREATE INDEX idx_consultation_messages_consultation_time
    ON consultation_messages (consultation_id, sent_at DESC, id DESC);
CREATE INDEX idx_consultation_messages_sender
    ON consultation_messages (sender_user_id);

CREATE TABLE consultation_clinical_notes (
    id UUID PRIMARY KEY,
    consultation_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_consultation_clinical_notes_consultation UNIQUE (consultation_id),
    CONSTRAINT fk_consultation_clinical_notes_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation_sessions (id),
    CONSTRAINT fk_consultation_clinical_notes_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id)
);

CREATE INDEX idx_consultation_clinical_notes_doctor
    ON consultation_clinical_notes (doctor_id);

INSERT INTO consultation_sessions (
    id,
    appointment_id,
    status,
    started_at,
    completed_at,
    cancelled_at,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    appointment.id,
    'SCHEDULED',
    NULL,
    NULL,
    NULL,
    COALESCE(appointment.confirmed_at, appointment.updated_at, CURRENT_TIMESTAMP),
    COALESCE(appointment.confirmed_at, appointment.updated_at, CURRENT_TIMESTAMP)
FROM appointments appointment
WHERE appointment.status = 'CONFIRMED'
  AND NOT EXISTS (
      SELECT 1
      FROM consultation_sessions consultation
      WHERE consultation.appointment_id = appointment.id
  );
