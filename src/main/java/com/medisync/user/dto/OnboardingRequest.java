package com.medisync.user.dto;

import com.medisync.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be 100 characters or fewer")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be 100 characters or fewer")
        String lastName,

        @Size(max = 30, message = "Phone number must be 30 characters or fewer")
        @Pattern(regexp = "^[+0-9() .-]*$", message = "Phone number contains unsupported characters")
        String phone,

        @NotNull(message = "Account type is required")
        UserRole role
) {
}
