package com.medisync.consultation.entity;

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
@Table(name = "consultation_sessions")
public class ConsultationSession {

    @Id
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConsultationStatus status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ConsultationSession() {
    }

    public ConsultationSession(UUID appointmentId) {
        this.id = UUID.randomUUID();
        this.appointmentId = appointmentId;
        this.status = ConsultationStatus.SCHEDULED;
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

    public void start() {
        requireStatus(ConsultationStatus.SCHEDULED, "Only a scheduled consultation can be started");
        status = ConsultationStatus.IN_PROGRESS;
        startedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void complete() {
        requireStatus(ConsultationStatus.IN_PROGRESS, "Only an in-progress consultation can be completed");
        status = ConsultationStatus.COMPLETED;
        completedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void cancel() {
        requireStatus(ConsultationStatus.SCHEDULED,
                "An in-progress or completed consultation cannot be cancelled");
        status = ConsultationStatus.CANCELLED;
        cancelledAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void requireStatus(ConsultationStatus required, String message) {
        if (status != required) {
            throw new ResourceConflictException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getAppointmentId() { return appointmentId; }
    public ConsultationStatus getStatus() { return status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
