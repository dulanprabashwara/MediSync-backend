package com.medisync.doctor.dto;

import java.util.UUID;

public record DoctorSummaryResponse(
        UUID doctorProfileId,
        String displayName,
        String phone,
        UUID hospitalId,
        String hospitalName,
        UUID departmentId,
        String departmentName,
        UUID specializationId,
        String specializationName,
        String qualifications,
        Integer yearsOfExperience,
        String bioSummary,
        String profileImageUrl,
        boolean verified
) {
}
