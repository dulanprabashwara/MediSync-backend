package com.medisync.prescription.entity;

import com.medisync.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionDoctorFeeTest {

    @Test
    void positiveFeeRequiresConfirmationBeforeQrEligibility() {
        Prescription prescription = prescription();
        prescription.updateDraft(30, null, new BigDecimal("2500.00"), "LKR");
        prescription.issue(OffsetDateTime.now(ZoneOffset.UTC));

        assertThat(prescription.getDoctorFeeStatus()).isEqualTo(DoctorFeeStatus.AWAITING_CONFIRMATION);
        assertThat(prescription.isQrPaymentEligible()).isFalse();

        UUID doctorUserId = UUID.randomUUID();
        prescription.confirmDoctorFee(doctorUserId, OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(prescription.getDoctorFeeStatus()).isEqualTo(DoctorFeeStatus.CONFIRMED);
        assertThat(prescription.getDoctorFeeConfirmedBy()).isEqualTo(doctorUserId);
        assertThat(prescription.isQrPaymentEligible()).isTrue();
    }

    @Test
    void zeroFeeRemainsBackwardCompatibleAndNeedsNoConfirmation() {
        Prescription prescription = prescription();
        prescription.updateDraft(30, null, BigDecimal.ZERO, "LKR");
        prescription.issue(OffsetDateTime.now(ZoneOffset.UTC));

        assertThat(prescription.getDoctorFeeStatus()).isEqualTo(DoctorFeeStatus.NOT_REQUIRED);
        assertThat(prescription.isQrPaymentEligible()).isTrue();
        assertThatThrownBy(() -> prescription.confirmDoctorFee(UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC)))
                .isInstanceOf(ResourceConflictException.class);
    }

    private Prescription prescription() {
        return new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
