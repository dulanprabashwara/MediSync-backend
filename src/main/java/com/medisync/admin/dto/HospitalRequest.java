package com.medisync.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HospitalRequest(
        @NotBlank(message = "Hospital name is required")
        @Size(max = 200, message = "Hospital name must be 200 characters or fewer")
        String name,

        @Size(max = 255, message = "Address must be 255 characters or fewer")
        String addressLine,

        @Size(max = 100, message = "City must be 100 characters or fewer")
        String city,

        @Size(max = 30, message = "Phone number must be 30 characters or fewer")
        @Pattern(regexp = "^[+0-9() .-]*$", message = "Phone number contains unsupported characters")
        String phone,

        Boolean active
) {
}

