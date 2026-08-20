package com.medisync.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecializationRequest(
        @NotBlank(message = "Specialization name is required")
        @Size(max = 150, message = "Specialization name must be 150 characters or fewer")
        String name,

        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,

        Boolean active
) {
}

