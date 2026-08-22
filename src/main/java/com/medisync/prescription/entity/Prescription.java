package com.medisync.prescription.entity;

import com.medisync.exception.ResourceConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    private UUID id;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PrescriptionStatus status;

    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    @Column(name = "general_instructions", columnDefinition = "TEXT")
    private String generalInstructions;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "doctor_fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal doctorFeeAmount;

    @Column(name = "doctor_fee_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String doctorFeeCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "doctor_fee_status", nullable = false, length = 32)
    private DoctorFeeStatus doctorFeeStatus;

    @Column(name = "doctor_fee_confirmed_at")
    private OffsetDateTime doctorFeeConfirmedAt;

    @Column(name = "doctor_fee_confirmed_by")
    private UUID doctorFeeConfirmedBy;

    protected Prescription() {
    }

    public Prescription(UUID consultationId, UUID doctorId, UUID patientId) {
        this.id = UUID.randomUUID();
        this.consultationId = consultationId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.status = PrescriptionStatus.DRAFT;
        this.validityDays = 30;
        this.doctorFeeAmount = BigDecimal.ZERO.setScale(2);
        this.doctorFeeCurrency = "LKR";
        this.doctorFeeStatus = DoctorFeeStatus.NOT_REQUIRED;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    }

    public void updateDraft(int validityDays, String generalInstructions,
                            BigDecimal doctorFeeAmount, String doctorFeeCurrency) {
        requireStatus(PrescriptionStatus.DRAFT, "Only a draft prescription can be edited");
        this.validityDays = validityDays;
        this.generalInstructions = generalInstructions;
        this.doctorFeeAmount = doctorFeeAmount.setScale(2);
        this.doctorFeeCurrency = doctorFeeCurrency;
        this.doctorFeeStatus = doctorFeeAmount.signum() == 0
                ? DoctorFeeStatus.NOT_REQUIRED
                : DoctorFeeStatus.AWAITING_CONFIRMATION;
    }

    public void updateDraft(int validityDays, String generalInstructions) {
        updateDraft(validityDays, generalInstructions, BigDecimal.ZERO, "LKR");
    }

    public void issue(OffsetDateTime now) {
        requireStatus(PrescriptionStatus.DRAFT, "This prescription has already been finalized");
        status = PrescriptionStatus.ISSUED;
        issuedAt = now;
        validUntil = now.plusDays(validityDays);
        doctorFeeStatus = doctorFeeAmount.signum() == 0
                ? DoctorFeeStatus.NOT_REQUIRED
                : DoctorFeeStatus.AWAITING_CONFIRMATION;
    }

    public void confirmDoctorFee(UUID doctorUserId, OffsetDateTime now) {
        requireStatus(PrescriptionStatus.ISSUED, "Only an issued prescription can have payment confirmed");
        if (doctorFeeStatus != DoctorFeeStatus.AWAITING_CONFIRMATION) {
            throw new ResourceConflictException("This prescription is not awaiting payment confirmation");
        }
        doctorFeeStatus = DoctorFeeStatus.CONFIRMED;
        doctorFeeConfirmedAt = now;
        doctorFeeConfirmedBy = doctorUserId;
    }

    public boolean isQrPaymentEligible() {
        return doctorFeeStatus == DoctorFeeStatus.NOT_REQUIRED
                || doctorFeeStatus == DoctorFeeStatus.CONFIRMED;
    }

    public void cancel(String reason, OffsetDateTime now) {
        requireStatus(PrescriptionStatus.ISSUED, "Only an issued prescription can be cancelled");
        status = PrescriptionStatus.CANCELLED;
        cancellationReason = reason;
        cancelledAt = now;
    }

    private void requireStatus(PrescriptionStatus required, String message) {
        if (status != required) {
            throw new ResourceConflictException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getConsultationId() { return consultationId; }
    public UUID getDoctorId() { return doctorId; }
    public UUID getPatientId() { return patientId; }
    public PrescriptionStatus getStatus() { return status; }
    public int getValidityDays() { return validityDays; }
    public String getGeneralInstructions() { return generalInstructions; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getValidUntil() { return validUntil; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public BigDecimal getDoctorFeeAmount() { return doctorFeeAmount; }
    public String getDoctorFeeCurrency() { return doctorFeeCurrency; }
    public DoctorFeeStatus getDoctorFeeStatus() { return doctorFeeStatus; }
    public OffsetDateTime getDoctorFeeConfirmedAt() { return doctorFeeConfirmedAt; }
    public UUID getDoctorFeeConfirmedBy() { return doctorFeeConfirmedBy; }
}
