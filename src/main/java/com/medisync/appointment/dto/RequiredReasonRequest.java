package com.medisync.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequiredReasonRequest(
        @NotBlank @Size(min = 3, max = 1000) String reason
) {
}
