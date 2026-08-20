package com.medisync.consultation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClinicalNoteRequest(
        @NotNull(message = "Clinical note text is required")
        @Size(max = 20000, message = "Clinical note must not exceed 20000 characters")
        String noteText
) {
}
