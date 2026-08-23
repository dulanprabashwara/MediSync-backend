package com.medisync.prescription.dto;

import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.pharmacy.dto.DispensingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import com.medisync.prescription.entity.DoctorFeeStatus;

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
        BigDecimal doctorFeeAmount,
        String doctorFeeCurrency,
        DoctorFeeStatus doctorFeeStatus,
        OffsetDateTime doctorFeeConfirmedAt,
        String doctorBankAccountHolder,
        String doctorBankName,
        String doctorBankBranch,
        String doctorBankAccountNumber,
        List<PrescriptionItemResponse> items,
        OffsetDateTime issuedAt,
        OffsetDateTime validUntil,
        boolean expired,
        DispensingStatus dispensingStatus,
        OffsetDateTime dispensedAt,
        String dispensingPharmacy,
        OffsetDateTime cancelledAt,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean cancellationAllowed,
        String cancellationBlockedReason
) {
}
