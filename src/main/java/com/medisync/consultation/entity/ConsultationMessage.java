package com.medisync.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "consultation_messages")
public class ConsultationMessage {

    @Id
    private UUID id;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected ConsultationMessage() {
    }

    public ConsultationMessage(UUID consultationId, UUID senderUserId, String content) {
        this.id = UUID.randomUUID();
        this.consultationId = consultationId;
        this.senderUserId = senderUserId;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        sentAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public UUID getId() { return id; }
    public UUID getConsultationId() { return consultationId; }
    public UUID getSenderUserId() { return senderUserId; }
    public String getContent() { return content; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
}
