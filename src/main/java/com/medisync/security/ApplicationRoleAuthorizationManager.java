package com.medisync.security;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ApplicationRoleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AppUserRepository appUserRepository;
    private final ProfessionalVerificationAccess professionalVerificationAccess;
    private final UserRole requiredRole;
    private final boolean allowPendingVerification;

    public ApplicationRoleAuthorizationManager(AppUserRepository appUserRepository,
                                               ProfessionalVerificationAccess professionalVerificationAccess,
                                               UserRole requiredRole) {
        this(appUserRepository, professionalVerificationAccess, requiredRole, false);
    }

    public ApplicationRoleAuthorizationManager(
            AppUserRepository appUserRepository,
            ProfessionalVerificationAccess professionalVerificationAccess,
            UserRole requiredRole,
            boolean allowPendingVerification
    ) {
        this.appUserRepository = appUserRepository;
        this.professionalVerificationAccess = professionalVerificationAccess;
        this.requiredRole = requiredRole;
        this.allowPendingVerification = allowPendingVerification;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context
    ) {
        Authentication authentication = authenticationSupplier.get();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        try {
            UUID authUserId = UUID.fromString(jwtAuthentication.getToken().getSubject());
            Optional<AppUser> user = appUserRepository.findByAuthUserId(authUserId);
            boolean granted = user
                    .filter(candidate -> candidate.getRole() == requiredRole)
                    .filter(candidate -> candidate.getStatus() == AccountStatus.ACTIVE
                            || (allowPendingVerification
                            && candidate.getStatus() == AccountStatus.PENDING_VERIFICATION))
                    .filter(candidate -> allowPendingVerification
                            || (requiredRole != UserRole.DOCTOR && requiredRole != UserRole.PHARMACIST)
                            || professionalVerificationAccess.isVerified(candidate))
                    .isPresent();
            return new AuthorizationDecision(granted);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return new AuthorizationDecision(false);
        }
    }
}
