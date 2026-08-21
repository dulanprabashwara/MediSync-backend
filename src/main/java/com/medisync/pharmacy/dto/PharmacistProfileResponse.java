package com.medisync.pharmacy.dto;

import com.medisync.user.entity.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PharmacistProfileResponse(
        UUID id,
        String professionalRegistrationNumber,
        String pharmacyName,
        String pharmacyRegistrationNumber,
        String pharmacyAddress,
        String qualifications,
        VerificationStatus verificationStatus,
        String verificationRejectionReason,
        OffsetDateTime submittedForVerificationAt,
        OffsetDateTime verifiedAt,
        boolean profileComplete,
        boolean submitted,
        boolean editable,
        boolean pharmacyAccessAllowed
) {
}
