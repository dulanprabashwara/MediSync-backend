package com.medisync.admin.dto;

import java.util.UUID;

public record AdminActivityRanking(
        UUID userId,
        String displayName,
        long activityCount
) {
}
