package com.medisync.audit.entity;

import com.medisync.user.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Map;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 32)
    private UserRole actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected AuditEvent() {
    }

    public AuditEvent(OffsetDateTime occurredAt, UUID actorUserId, UserRole actorRole,
                      String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        this.id = UUID.randomUUID();
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public UUID getActorUserId() { return actorUserId; }
    public UserRole getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Map<String, Object> getMetadata() { return metadata; }
}
