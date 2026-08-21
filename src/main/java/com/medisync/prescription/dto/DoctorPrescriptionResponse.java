package com.medisync.prescription.dto;

import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.consultation.entity.ConsultationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DoctorPrescriptionResponse(
        UUID id,
        UUID consultationId,
        ConsultationStatus consultationStatus,
        PrescriptionStatus status,
        String patientName,
        String doctorName,
        String medicalRegistrationNumber,
        String hospitalName,
        String departmentName,
        String specializationName,
        OffsetDateTime consultationScheduledStart,
        int validityDays,
        String generalInstructions,
        List<PrescriptionItemResponse> items,
        OffsetDateTime issuedAt,
        OffsetDateTime validUntil,
        boolean expired,
        OffsetDateTime cancelledAt,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
