package com.medisync.user.service;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.exception.InvalidAuthenticatedUserException;
import com.medisync.user.exception.OnboardingRequiredException;
import com.medisync.user.repository.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser requireCurrentUser(Jwt jwt) {
        UUID authUserId;
        try {
            authUserId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidAuthenticatedUserException("The authenticated user identifier is invalid");
        }
        return appUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(OnboardingRequiredException::new);
    }

    public AppUser requireRole(Jwt jwt, UserRole role, AccountStatus... allowedStatuses) {
        AppUser user = requireCurrentUser(jwt);
        boolean allowedStatus = Arrays.stream(allowedStatuses).anyMatch(status -> status == user.getStatus());
        if (user.getRole() != role || !allowedStatus) {
            throw new AccessDeniedException("This account cannot access the requested resource");
        }
        return user;
    }
}

