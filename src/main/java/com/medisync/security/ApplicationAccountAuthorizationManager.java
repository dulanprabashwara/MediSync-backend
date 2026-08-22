package com.medisync.security;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.repository.AppUserRepository;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.UUID;
import java.util.function.Supplier;

public class ApplicationAccountAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AppUserRepository repository;

    public ApplicationAccountAuthorizationManager(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                       RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        try {
            UUID authUserId = UUID.fromString(jwt.getToken().getSubject());
            boolean allowed = repository.findByAuthUserId(authUserId)
                    .filter(user -> user.getStatus() == AccountStatus.ACTIVE
                            || user.getStatus() == AccountStatus.PENDING_VERIFICATION)
                    .isPresent();
            return new AuthorizationDecision(allowed);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return new AuthorizationDecision(false);
        }
    }
}
