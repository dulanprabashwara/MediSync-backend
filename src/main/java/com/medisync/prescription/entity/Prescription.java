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

    protected Prescription() {
    }

    public Prescription(UUID consultationId, UUID doctorId, UUID patientId) {
        this.id = UUID.randomUUID();
        this.consultationId = consultationId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.status = PrescriptionStatus.DRAFT;
        this.validityDays = 30;
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

    public void updateDraft(int validityDays, String generalInstructions) {
        requireStatus(PrescriptionStatus.DRAFT, "Only a draft prescription can be edited");
        this.validityDays = validityDays;
        this.generalInstructions = generalInstructions;
    }

    public void issue(OffsetDateTime now) {
        requireStatus(PrescriptionStatus.DRAFT, "This prescription has already been finalized");
        status = PrescriptionStatus.ISSUED;
        issuedAt = now;
        validUntil = now.plusDays(validityDays);
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
}
