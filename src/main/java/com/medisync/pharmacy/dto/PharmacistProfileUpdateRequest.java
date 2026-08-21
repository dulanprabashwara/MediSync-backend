package com.medisync.pharmacy.dto;

import jakarta.validation.constraints.Size;

public record PharmacistProfileUpdateRequest(
        @Size(max = 100, message = "Professional registration number must be 100 characters or fewer")
        String professionalRegistrationNumber,

        @Size(max = 200, message = "Pharmacy name must be 200 characters or fewer")
        String pharmacyName,

        @Size(max = 100, message = "Pharmacy registration number must be 100 characters or fewer")
        String pharmacyRegistrationNumber,

        @Size(max = 500, message = "Pharmacy address must be 500 characters or fewer")
        String pharmacyAddress,

        @Size(max = 500, message = "Qualifications must be 500 characters or fewer")
        String qualifications
) {
}
