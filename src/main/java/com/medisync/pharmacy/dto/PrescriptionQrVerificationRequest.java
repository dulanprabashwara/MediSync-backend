package com.medisync.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionQrVerificationRequest(
        @NotBlank(message = "A prescription QR payload is required")
        @Size(max = 128, message = "The prescription QR payload is invalid")
        String qrPayload
) {
}
