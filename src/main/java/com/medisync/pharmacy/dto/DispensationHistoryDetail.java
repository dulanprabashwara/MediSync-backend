package com.medisync.pharmacy.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DispensationHistoryDetail(
        UUID id,
        OffsetDateTime dispensedAt,
        String patientName,
        String doctorName,
        String pharmacyName,
        String pharmacistRegistrationNumber,
        String dispensingNote,
        OffsetDateTime issuedAt,
        OffsetDateTime validUntil,
        List<DispensingPrescriptionItemResponse> items,
        String generalInstructions
) {
}
