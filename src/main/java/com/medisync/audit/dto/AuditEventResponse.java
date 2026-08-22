package com.medisync.audit.dto;

import com.medisync.user.entity.UserRole;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        OffsetDateTime occurredAt,
        UUID actorUserId,
        UserRole actorRole,
        String action,
        String targetType,
        UUID targetId,
        Map<String, Object> metadata
) {
}
