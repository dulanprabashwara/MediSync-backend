package com.medisync.pharmacy.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PharmacyPrescriptionVerificationResponse(
        PharmacyVerificationStatus status,
        String message,
        boolean dispensingEligible,
        String patientName,
        String doctorName,
        String doctorRegistrationNumber,
        String doctorSpecialization,
        String affiliatedHospital,
        OffsetDateTime issuedAt,
        OffsetDateTime validUntil,
        List<DispensingPrescriptionItemResponse> items,
        String generalInstructions,
        OffsetDateTime dispensedAt,
        String pharmacyName
) {
}
