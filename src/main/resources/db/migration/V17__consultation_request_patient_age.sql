ALTER TABLE appointment_symptoms
    ADD COLUMN patient_age INTEGER;

ALTER TABLE appointment_symptoms
    ADD CONSTRAINT chk_appointment_symptoms_patient_age
        CHECK (patient_age IS NULL OR patient_age BETWEEN 0 AND 130);
