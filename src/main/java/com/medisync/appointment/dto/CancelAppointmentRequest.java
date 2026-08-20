package com.medisync.appointment.dto;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(@Size(max = 1000) String reason) {
}
