package com.medisync.admin.dto;

import com.medisync.department.entity.Department;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID hospitalId,
        String hospitalName,
        String name,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DepartmentResponse from(Department department, String hospitalName) {
        return new DepartmentResponse(department.getId(), department.getHospitalId(), hospitalName,
                department.getName(), department.isActive(), department.getCreatedAt(), department.getUpdatedAt());
    }
}

