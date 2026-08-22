package com.medisync.admin.dto;

import com.medisync.specialization.entity.Specialization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SpecializationResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long doctorCount
) {
    public static SpecializationResponse from(Specialization specialization) {
        return from(specialization, 0);
    }

    public static SpecializationResponse from(Specialization specialization, long doctorCount) {
        return new SpecializationResponse(specialization.getId(), specialization.getName(),
                specialization.getDescription(), specialization.isActive(), specialization.getCreatedAt(),
                specialization.getUpdatedAt(), doctorCount);
    }
}
