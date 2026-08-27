package com.medisync.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID slotId,
        @NotNull @Min(0) @Max(130) Integer patientAge,
        @NotBlank @Size(max = 300) String reasonForVisit,
        @NotBlank @Size(max = 2000) String symptoms,
        @Size(max = 200) String symptomDuration,
        @Size(max = 2000) String additionalNotes
) {
}
