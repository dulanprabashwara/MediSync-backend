package com.medisync.user.exception;

public class InvalidOnboardingRoleException extends RuntimeException {
    public InvalidOnboardingRoleException() {
        super("Choose Patient, Doctor, or Pharmacist as the account type");
    }
}
