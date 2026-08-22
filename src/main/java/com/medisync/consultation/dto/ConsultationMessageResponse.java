package com.medisync.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

public record ConsultationMessageResponse(
        UUID messageId,
        UUID consultationId,
        ConsultationSenderType senderType,
        String senderDisplayName,
        String senderProfileImageUrl,
        String content,
        List<ConsultationMessageAttachmentResponse> attachments,
        OffsetDateTime sentAt
) {
    public ConsultationMessageResponse(UUID messageId, UUID consultationId,
                                       ConsultationSenderType senderType, String senderDisplayName,
                                       String content, OffsetDateTime sentAt) {
        this(messageId, consultationId, senderType, senderDisplayName, null, content, List.of(), sentAt);
    }
}
