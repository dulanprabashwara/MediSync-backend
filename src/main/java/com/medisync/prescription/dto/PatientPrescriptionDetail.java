package com.medisync.prescription.dto;

import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.pharmacy.dto.DispensingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import com.medisync.prescription.entity.DoctorFeeStatus;

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
        DispensingStatus dispensingStatus,
        BigDecimal doctorFeeAmount,
        String doctorFeeCurrency,
        DoctorFeeStatus doctorFeeStatus,
        OffsetDateTime doctorFeeConfirmedAt,
        String doctorBankAccountHolder,
        String doctorBankName,
        String doctorBankBranch,
        String doctorBankAccountNumber,
        OffsetDateTime dispensedAt,
        String dispensingPharmacy,
        String generalInstructions,
        List<PrescriptionItemResponse> items,
        String cancellationReason,
        OffsetDateTime cancelledAt,
        boolean qrGenerationAllowed
) {
}
