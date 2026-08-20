package com.medisync.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationMessageResponse(
        UUID messageId,
        UUID consultationId,
        ConsultationSenderType senderType,
        String senderDisplayName,
        String content,
        OffsetDateTime sentAt
) {
}
