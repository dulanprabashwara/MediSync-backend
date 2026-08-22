package com.medisync.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

public record PrescriptionDraftRequest(
        @Min(1) @Max(90) int validityDays,
        @Size(max = 5000) String generalInstructions,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal doctorFeeAmount,
        @NotNull @Size(max = 20) List<@Valid PrescriptionItemRequest> items
) {
    public PrescriptionDraftRequest(int validityDays, String generalInstructions,
                                    List<PrescriptionItemRequest> items) {
        this(validityDays, generalInstructions, BigDecimal.ZERO, items);
    }
}
