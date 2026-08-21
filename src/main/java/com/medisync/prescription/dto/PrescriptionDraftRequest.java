package com.medisync.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PrescriptionDraftRequest(
        @Min(1) @Max(90) int validityDays,
        @Size(max = 5000) String generalInstructions,
        @NotNull @Size(max = 20) List<@Valid PrescriptionItemRequest> items
) {
}
