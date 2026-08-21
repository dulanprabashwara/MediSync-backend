package com.medisync.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPharmacistRequest(
        @NotBlank(message = "A rejection reason is required")
        @Size(max = 1000, message = "Rejection reason must be 1000 characters or fewer")
        String reason
) {
}
