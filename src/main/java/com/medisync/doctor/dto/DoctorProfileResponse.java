package com.medisync.doctor.dto;

import com.medisync.user.entity.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DoctorProfileResponse(
        UUID id,
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
        OffsetDateTime verifiedAt,
        boolean profileComplete,
        boolean submitted,
        boolean editable,
        String bankAccountHolder,
        String bankName,
        String bankBranch,
        String bankAccountNumber
) {
}

