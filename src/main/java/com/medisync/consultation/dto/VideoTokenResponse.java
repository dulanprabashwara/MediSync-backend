package com.medisync.consultation.dto;

/**
 * Response containing everything the frontend needs to connect to a LiveKit room.
 * No clinical data, PII, or API secrets are included.
 */
public record VideoTokenResponse(
        String token,
        String serverUrl
) {
}
