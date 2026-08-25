package com.medisync.notification.dto;

import com.medisync.notification.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationDto {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private String entityType;
    private UUID entityId;
    private boolean read;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
    
    public NotificationDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
