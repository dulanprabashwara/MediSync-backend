package com.medisync.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClinicalNoteResponse(
        UUID consultationId,
        String noteText,
        boolean finalized,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
