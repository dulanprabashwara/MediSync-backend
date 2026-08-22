package com.medisync.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAccountDeletionRequest(
        @NotBlank(message = "Deletion reason is required")
        @Size(min = 3, max = 1000, message = "Reason must be between 3 and 1000 characters")
        String reason
) {
}
