package com.medisync.prescription.dto;

import com.medisync.prescription.entity.PrescriptionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PatientPrescriptionDetail(
        UUID id,
        UUID consultationId,
        String patientName,
        String doctorName,
        String medicalRegistrationNumber,
        String hospitalName,
        String departmentName,
        String specializationName,
        OffsetDateTime consultationScheduledStart,
        OffsetDateTime issuedAt,
        OffsetDateTime validUntil,
        PrescriptionStatus status,
        boolean expired,
        String generalInstructions,
        List<PrescriptionItemResponse> items,
        String cancellationReason,
        OffsetDateTime cancelledAt,
        String qrPayload,
        boolean qrUsable
) {
}
