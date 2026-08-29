package com.medisync.appointment.entity;

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
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "scheduled_start", nullable = false)
    private OffsetDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private OffsetDateTime scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppointmentStatus status;

    @Column(name = "doctor_rejection_reason", length = 1000)
    private String doctorRejectionReason;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected Appointment() {
    }

    public Appointment(UUID patientId, UUID doctorId, UUID slotId,
                       OffsetDateTime scheduledStart, OffsetDateTime scheduledEnd) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.slotId = slotId;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.status = AppointmentStatus.REQUESTED;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void confirm() {
        requireStatus(AppointmentStatus.REQUESTED);
        status = AppointmentStatus.CONFIRMED;
        confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void reject(String reason) {
        requireStatus(AppointmentStatus.REQUESTED);
        status = AppointmentStatus.REJECTED;
        doctorRejectionReason = reason;
        rejectedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void cancelByPatient(String reason) {
        if (status != AppointmentStatus.REQUESTED && status != AppointmentStatus.CONFIRMED) {
            throw new ResourceConflictException("This appointment can no longer be cancelled because it has already been processed");
        }
        status = AppointmentStatus.CANCELLED_BY_PATIENT;
        cancellationReason = reason;
        cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void cancelByDoctor(String reason) {
        requireStatus(AppointmentStatus.CONFIRMED);
        status = AppointmentStatus.CANCELLED_BY_DOCTOR;
        cancellationReason = reason;
        cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void requireStatus(AppointmentStatus required) {
        if (status != required) {
            throw new ResourceConflictException("The appointment has already been processed");
        }
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getDoctorId() { return doctorId; }
    public UUID getSlotId() { return slotId; }
    public OffsetDateTime getScheduledStart() { return scheduledStart; }
    public OffsetDateTime getScheduledEnd() { return scheduledEnd; }
    public AppointmentStatus getStatus() { return status; }
    public String getDoctorRejectionReason() { return doctorRejectionReason; }
    public String getCancellationReason() { return cancellationReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public OffsetDateTime getRejectedAt() { return rejectedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
}
