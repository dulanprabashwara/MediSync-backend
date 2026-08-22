package com.medisync.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanUserRequest(
        @NotBlank @Size(min = 3, max = 1000) String reason
) {
}
