package com.medisync.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDoctorRequest(
        @NotBlank(message = "A rejection reason is required")
        @Size(max = 1000, message = "Rejection reason must be 1000 characters or fewer")
        String reason
) {
}

