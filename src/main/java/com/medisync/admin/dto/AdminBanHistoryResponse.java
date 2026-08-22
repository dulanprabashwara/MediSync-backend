package com.medisync.admin.dto;

import com.medisync.user.entity.AccountStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminBanHistoryResponse(
        UUID id,
        String reason,
        AccountStatus previousStatus,
        UUID bannedBy,
        OffsetDateTime bannedAt,
        UUID unbannedBy,
        OffsetDateTime unbannedAt
) {
}
