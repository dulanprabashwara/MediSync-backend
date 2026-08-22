package com.medisync.consultation.dto;

import java.util.UUID;

public record ConsultationMessageAttachmentResponse(
        UUID id,
        String contentType,
        String originalFilename,
        long byteSize,
        int position,
        String signedUrl
) {
}
