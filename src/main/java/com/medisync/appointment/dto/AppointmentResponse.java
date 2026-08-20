package com.medisync.appointment.dto;

import com.medisync.appointment.entity.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID slotId,
        String patientName,
        String doctorName,
        String hospitalName,
        String departmentName,
        String specializationName,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        AppointmentStatus status,
        AppointmentSymptomsResponse symptoms,
        String doctorRejectionReason,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime rejectedAt,
        OffsetDateTime cancelledAt
) {
}
