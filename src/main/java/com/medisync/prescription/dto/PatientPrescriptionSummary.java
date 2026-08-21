package com.medisync.prescription.dto;

import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.pharmacy.dto.DispensingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatientPrescriptionSummary(
        UUID id,
        UUID consultationId,
        String doctorName,
        String specializationName,
        String hospitalName,
        OffsetDateTime issuedAt,
        OffsetDateTime validUntil,
        PrescriptionStatus status,
        boolean expired,
        DispensingStatus dispensingStatus,
        OffsetDateTime dispensedAt,
        String dispensingPharmacy,
        long medicineCount
) {
}
