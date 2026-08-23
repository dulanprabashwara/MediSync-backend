package com.medisync.consultation.dto;

import com.medisync.appointment.dto.AppointmentSymptomsResponse;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.consultation.entity.ConsultationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationResponse(
        UUID id,
        UUID appointmentId,
        ConsultationStatus status,
        AppointmentStatus appointmentStatus,
        String cancellationReason,
        String patientName,
        String doctorName,
        String hospitalName,
        String departmentName,
        String specializationName,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        AppointmentSymptomsResponse symptoms,
        boolean chatEnabled,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        java.util.List<ConsultationPaymentSummary> paymentSummaries
) {
}
