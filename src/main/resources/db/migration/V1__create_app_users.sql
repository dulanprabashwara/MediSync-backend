CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    auth_user_id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_users_auth_user_id UNIQUE (auth_user_id),
    CONSTRAINT chk_app_users_role CHECK (role IN ('PATIENT', 'DOCTOR', 'PHARMACIST', 'ADMIN')),
    CONSTRAINT chk_app_users_status CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED', 'DISABLED'))
);

CREATE INDEX idx_app_users_role ON app_users (role);
CREATE INDEX idx_app_users_status ON app_users (status);

