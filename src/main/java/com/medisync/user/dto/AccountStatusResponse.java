package com.medisync.user.dto;

import com.medisync.user.entity.AccountStatus;

import java.time.OffsetDateTime;

public record AccountStatusResponse(
        AccountStatus status,
        String restrictionReason,
        OffsetDateTime restrictedAt
) {
}
