package com.medisync.admin.dto;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserSummary(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        AccountStatus status,
        VerificationStatus verificationStatus,
        String professionalRegistrationNumber,
        String hospitalName,
        String departmentName,
        String specializationName,
        String pharmacyName,
        String profileImageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastRecordedActivityAt
) {
}
