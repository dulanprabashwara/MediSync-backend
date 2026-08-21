package com.medisync.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DispensePrescriptionRequest(
        @NotBlank(message = "A prescription QR payload is required")
        @Size(max = 128, message = "The prescription QR payload is invalid")
        String qrPayload,

        @Size(max = 1000, message = "Dispensing note must be 1000 characters or fewer")
        String note
) {
}
