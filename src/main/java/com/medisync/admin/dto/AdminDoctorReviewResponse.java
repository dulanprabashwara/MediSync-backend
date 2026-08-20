package com.medisync.admin.dto;

import com.medisync.user.entity.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDoctorReviewResponse(
        UUID doctorId,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String medicalRegistrationNumber,
        UUID hospitalId,
        String hospitalName,
        UUID departmentId,
        String departmentName,
        UUID specializationId,
        String specializationName,
        String qualifications,
        Integer yearsOfExperience,
        String bio,
        VerificationStatus verificationStatus,
        String verificationRejectionReason,
        OffsetDateTime submittedForVerificationAt,
        OffsetDateTime verifiedAt
) {
}

