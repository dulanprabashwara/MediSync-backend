package com.medisync.availability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "doctor_availability_windows")
public class DoctorAvailabilityWindow {

    @Id
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DoctorAvailabilityWindow() {
    }

    public DoctorAvailabilityWindow(UUID doctorId, OffsetDateTime startsAt, OffsetDateTime endsAt,
                                    int slotDurationMinutes, String timeZone) {
        this.id = UUID.randomUUID();
        this.doctorId = doctorId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.slotDurationMinutes = slotDurationMinutes;
        this.timeZone = timeZone;
        this.active = true;
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

    public void deactivate() {
        active = false;
    }

    public UUID getId() { return id; }
    public UUID getDoctorId() { return doctorId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public int getSlotDurationMinutes() { return slotDurationMinutes; }
    public String getTimeZone() { return timeZone; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
