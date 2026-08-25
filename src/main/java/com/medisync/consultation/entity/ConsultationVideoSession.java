package com.medisync.consultation.entity;

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
@Table(name = "consultation_video_sessions")
public class ConsultationVideoSession {

    @Id
    private UUID id;

    @Column(name = "consultation_id", nullable = false, unique = true)
    private UUID consultationId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_room_name", nullable = false, unique = true, length = 100)
    private String providerRoomName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VideoSessionStatus status;

    @Column(name = "started_by_user_id", nullable = false)
    private UUID startedByUserId;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum VideoSessionStatus {
        ACTIVE,
        ENDED
    }

    protected ConsultationVideoSession() {
    }

    public ConsultationVideoSession(UUID consultationId, String providerRoomName, UUID startedByUserId) {
        this.id = UUID.randomUUID();
        this.consultationId = consultationId;
        this.provider = "LIVEKIT";
        this.providerRoomName = providerRoomName;
        this.status = VideoSessionStatus.ACTIVE;
        this.startedByUserId = startedByUserId;
        this.startedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void end() {
        this.status = VideoSessionStatus.ENDED;
        this.endedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getConsultationId() { return consultationId; }
    public String getProvider() { return provider; }
    public String getProviderRoomName() { return providerRoomName; }
    public VideoSessionStatus getStatus() { return status; }
    public UUID getStartedByUserId() { return startedByUserId; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
