package com.medisync.pharmacy.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DispensationHistorySummary(
        UUID id,
        OffsetDateTime dispensedAt,
        String patientName,
        String doctorName,
        String pharmacyName,
        long medicineCount
) {
}
