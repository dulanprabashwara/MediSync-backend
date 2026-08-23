-- Add payment information to doctor profiles
ALTER TABLE doctor_profiles
ADD COLUMN bank_account_holder VARCHAR(200),
ADD COLUMN bank_name VARCHAR(100),
ADD COLUMN bank_branch VARCHAR(100),
ADD COLUMN bank_account_number VARCHAR(50);

-- Add payment information snapshot to prescriptions
ALTER TABLE prescriptions
ADD COLUMN doctor_bank_account_holder VARCHAR(200),
ADD COLUMN doctor_bank_name VARCHAR(100),
ADD COLUMN doctor_bank_branch VARCHAR(100),
ADD COLUMN doctor_bank_account_number VARCHAR(50);
