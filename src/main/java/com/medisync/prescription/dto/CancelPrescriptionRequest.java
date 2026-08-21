package com.medisync.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelPrescriptionRequest(
        @NotBlank @Size(min = 3, max = 1000) String reason
) {
}
