package com.medisync.user.exception;

public class DuplicateOnboardingException extends RuntimeException {
    public DuplicateOnboardingException() {
        super("A MediSync profile already exists for this account");
    }
}
