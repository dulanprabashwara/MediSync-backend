package com.medisync.consultation.dto;

import com.medisync.prescription.entity.DoctorFeeStatus;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.pharmacy.dto.DispensingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationPaymentSummary(
        UUID prescriptionId,
        PrescriptionStatus prescriptionStatus,
        BigDecimal doctorFeeAmount,
        String doctorFeeCurrency,
        DoctorFeeStatus doctorPaymentStatus,
        OffsetDateTime paymentConfirmedAt,
        boolean qrGenerationAllowed,
        DispensingStatus dispensingStatus,
        String doctorBankAccountHolder,
        String doctorBankName,
        String doctorBankBranch,
        String doctorBankAccountNumber
) {
}
