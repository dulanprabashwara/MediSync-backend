package com.medisync.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DepartmentRequest(
        @NotNull(message = "Hospital is required")
        UUID hospitalId,

        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name must be 150 characters or fewer")
        String name,

        Boolean active
) {
}

