package com.medisync.admin.dto;

import com.medisync.hospital.entity.Hospital;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HospitalResponse(
        UUID id,
        String name,
        String addressLine,
        String city,
        String phone,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long doctorCount
) {
    public static HospitalResponse from(Hospital hospital) {
        return from(hospital, 0);
    }

    public static HospitalResponse from(Hospital hospital, long doctorCount) {
        return new HospitalResponse(hospital.getId(), hospital.getName(), hospital.getAddressLine(),
                hospital.getCity(), hospital.getPhone(), hospital.isActive(), hospital.getCreatedAt(),
                hospital.getUpdatedAt(), doctorCount);
    }
}
