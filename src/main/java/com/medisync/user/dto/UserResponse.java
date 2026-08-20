package com.medisync.user.dto;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID authUserId,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        AccountStatus status
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getAuthUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}
