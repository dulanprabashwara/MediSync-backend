ALTER TABLE specializations
    ADD COLUMN department_id UUID,
    ADD CONSTRAINT fk_specializations_department
        FOREIGN KEY (department_id) REFERENCES departments (id);

DROP INDEX IF EXISTS uk_specializations_name_ci;

CREATE UNIQUE INDEX uk_specializations_department_name_ci
    ON specializations (department_id, LOWER(name))
    WHERE department_id IS NOT NULL;

CREATE UNIQUE INDEX uk_specializations_legacy_name_ci
    ON specializations (LOWER(name))
    WHERE department_id IS NULL;

CREATE INDEX idx_specializations_department_active_name
    ON specializations (department_id, active, name);
