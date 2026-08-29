package com.medisync.admin.dto;

import com.medisync.specialization.entity.Specialization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SpecializationResponse(
        UUID id,
        UUID hospitalId,
        String hospitalName,
        UUID departmentId,
        String departmentName,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long doctorCount
) {
    public static SpecializationResponse from(Specialization specialization) {
        return from(specialization, null, null, null, 0);
    }

    public static SpecializationResponse from(Specialization specialization, long doctorCount) {
        return from(specialization, null, null, null, doctorCount);
    }

    public static SpecializationResponse from(Specialization specialization, UUID hospitalId, String hospitalName,
                                               String departmentName, long doctorCount) {
        return new SpecializationResponse(specialization.getId(), hospitalId, hospitalName,
                specialization.getDepartmentId(), departmentName, specialization.getName(),
                specialization.getDescription(), specialization.isActive(), specialization.getCreatedAt(),
                specialization.getUpdatedAt(), doctorCount);
    }
}
