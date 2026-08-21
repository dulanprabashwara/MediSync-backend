package com.medisync.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(
        @NotBlank(message = "Medicine name is required") @Size(max = 200) String medicineName,
        @Size(max = 100) String strength,
        @Size(max = 100) String medicineForm,
        @NotBlank(message = "Dosage is required") @Size(max = 200) String dosage,
        @NotBlank(message = "Frequency is required") @Size(max = 200) String frequency,
        @NotBlank(message = "Duration is required") @Size(max = 200) String duration,
        @Size(max = 100) String quantity,
        @Size(max = 100) String route,
        @Size(max = 2000) String instructions
) {
}
