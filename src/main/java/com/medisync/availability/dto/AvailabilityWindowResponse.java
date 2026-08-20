package com.medisync.availability.dto;

import com.medisync.availability.entity.DoctorAvailabilityWindow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AvailabilityWindowResponse(
        UUID id,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        int slotDurationMinutes,
        String timeZone,
        boolean active,
        OffsetDateTime createdAt,
        List<AppointmentSlotResponse> slots
) {
    public static AvailabilityWindowResponse from(DoctorAvailabilityWindow window,
                                                  List<AppointmentSlotResponse> slots) {
        return new AvailabilityWindowResponse(window.getId(), window.getStartsAt(), window.getEndsAt(),
                window.getSlotDurationMinutes(), window.getTimeZone(), window.isActive(), window.getCreatedAt(), slots);
    }
}
