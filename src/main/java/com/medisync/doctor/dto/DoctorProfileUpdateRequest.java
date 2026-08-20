package com.medisync.doctor.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DoctorProfileUpdateRequest(
        @Size(max = 100, message = "Medical registration number must be 100 characters or fewer")
        String medicalRegistrationNumber,

        UUID hospitalId,
        UUID departmentId,
        UUID specializationId,

        @Size(max = 500, message = "Qualifications must be 500 characters or fewer")
        String qualifications,

        @PositiveOrZero(message = "Years of experience cannot be negative")
        Integer yearsOfExperience,

        @Size(max = 2000, message = "Bio must be 2000 characters or fewer")
        String bio
) {
}

