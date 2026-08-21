package com.medisync.pharmacy.dto;

import java.time.OffsetDateTime;

public record DispensePrescriptionResponse(
        String status,
        OffsetDateTime dispensedAt,
        String pharmacyName
) {
}
