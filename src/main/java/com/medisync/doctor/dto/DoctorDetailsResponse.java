package com.medisync.doctor.dto;

import java.util.UUID;

public record DoctorDetailsResponse(
        UUID doctorProfileId,
        String displayName,
        UUID hospitalId,
        String hospitalName,
        UUID departmentId,
        String departmentName,
        UUID specializationId,
        String specializationName,
        String qualifications,
        Integer yearsOfExperience,
        String bio,
        boolean verified
) {
}
