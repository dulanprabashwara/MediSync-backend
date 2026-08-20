package com.medisync.user.exception;

public class OnboardingRequiredException extends RuntimeException {
    public OnboardingRequiredException() {
        super("Complete onboarding to create your MediSync profile");
    }
}
