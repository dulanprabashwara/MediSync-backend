package com.medisync.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "consultation_message_attachments")
public class ConsultationMessageAttachment {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ConsultationMessageAttachment() {
    }

    public ConsultationMessageAttachment(UUID messageId, String storageKey, String contentType,
                                         String originalFilename, long byteSize, int position) {
        this.id = UUID.randomUUID();
        this.messageId = messageId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.byteSize = byteSize;
        this.position = position;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public String getOriginalFilename() { return originalFilename; }
    public long getByteSize() { return byteSize; }
    public int getPosition() { return position; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
