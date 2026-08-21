package com.medisync.pharmacy.dto;

import com.medisync.user.entity.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPharmacistReviewResponse(
        UUID pharmacistId,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String professionalRegistrationNumber,
        String pharmacyName,
        String pharmacyRegistrationNumber,
        String pharmacyAddress,
        String qualifications,
        VerificationStatus verificationStatus,
        String verificationRejectionReason,
        OffsetDateTime submittedForVerificationAt,
        OffsetDateTime verifiedAt
) {
}
