package com.medisync.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID slotId,
        @NotBlank @Size(max = 300) String reasonForVisit,
        @NotBlank @Size(max = 2000) String symptoms,
        @Size(max = 200) String symptomDuration,
        @Size(max = 2000) String additionalNotes
) {
}
