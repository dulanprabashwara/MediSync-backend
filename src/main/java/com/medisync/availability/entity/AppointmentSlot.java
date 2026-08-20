package com.medisync.availability.entity;

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
@Table(name = "appointment_slots")
public class AppointmentSlot {

    @Id
    private UUID id;

    @Column(name = "availability_window_id", nullable = false)
    private UUID availabilityWindowId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SlotStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AppointmentSlot() {
    }

    public AppointmentSlot(UUID availabilityWindowId, UUID doctorId,
                           OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.id = UUID.randomUUID();
        this.availabilityWindowId = availabilityWindowId;
        this.doctorId = doctorId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = SlotStatus.AVAILABLE;
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

    public void reserve() {
        requireStatus(SlotStatus.AVAILABLE, "This appointment slot is no longer available");
        status = SlotStatus.RESERVED;
    }

    public void book() {
        requireStatus(SlotStatus.RESERVED, "The appointment slot is not reserved");
        status = SlotStatus.BOOKED;
    }

    public void release() {
        if (status != SlotStatus.RESERVED && status != SlotStatus.BOOKED) {
            throw new ResourceConflictException("The appointment slot cannot be released from its current state");
        }
        status = SlotStatus.AVAILABLE;
    }

    public void block() {
        requireStatus(SlotStatus.AVAILABLE, "Only an available slot can be blocked");
        status = SlotStatus.BLOCKED;
    }

    public void unblock() {
        requireStatus(SlotStatus.BLOCKED, "Only a blocked slot can be unblocked");
        status = SlotStatus.AVAILABLE;
    }

    private void requireStatus(SlotStatus required, String message) {
        if (status != required) {
            throw new ResourceConflictException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getAvailabilityWindowId() { return availabilityWindowId; }
    public UUID getDoctorId() { return doctorId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public SlotStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
