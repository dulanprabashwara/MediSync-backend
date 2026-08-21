package com.medisync.prescription.dto;

import java.time.OffsetDateTime;

public record PrescriptionQrResponse(String qrPayload, OffsetDateTime expiresAt) {
}
