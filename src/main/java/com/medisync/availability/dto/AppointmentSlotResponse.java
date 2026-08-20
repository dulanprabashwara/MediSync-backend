package com.medisync.availability.dto;

import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.SlotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentSlotResponse(
        UUID id,
        UUID availabilityWindowId,
        UUID doctorId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        SlotStatus status
) {
    public static AppointmentSlotResponse from(AppointmentSlot slot) {
        return new AppointmentSlotResponse(slot.getId(), slot.getAvailabilityWindowId(), slot.getDoctorId(),
                slot.getStartsAt(), slot.getEndsAt(), slot.getStatus());
    }
}
