package com.medisync.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateAvailabilityRequest(
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @NotNull Integer slotDurationMinutes,
        @NotBlank @Size(max = 64) String timeZone
) {
}
