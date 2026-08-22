package com.medisync.user.dto;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;

import java.util.UUID;
import java.time.OffsetDateTime;

public record UserResponse(
        UUID id,
        UUID authUserId,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        AccountStatus status,
        String profileImageUrl,
        OffsetDateTime profileImageUpdatedAt
) {
    public static UserResponse from(AppUser user) {
        return from(user, null);
    }

    public static UserResponse from(AppUser user, String profileImageUrl) {
        return new UserResponse(
                user.getId(),
                user.getAuthUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                profileImageUrl,
                user.getProfileImageUpdatedAt()
        );
    }
}
