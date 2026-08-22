package com.medisync.security;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationRoleAuthorizationManagerTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);

    @Test
    void patientCannotAccessDoctorRoute() {
        UUID authUserId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(authUserId);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(user(authUserId, UserRole.PATIENT, AccountStatus.ACTIVE)));

        AuthorizationDecision decision = new ApplicationRoleAuthorizationManager(repository, UserRole.DOCTOR)
                .check(() -> authentication, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void matchingRoleCanAccessItsPortalWhilePendingVerification() {
        UUID authUserId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(authUserId);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                user(authUserId, UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION)));

        AuthorizationDecision decision = new ApplicationRoleAuthorizationManager(repository, UserRole.DOCTOR, true)
                .check(() -> authentication, context);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void pendingProfessionalCannotAccessActiveRoleFeatures() {
        UUID authUserId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(authUserId);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                user(authUserId, UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION)));

        AuthorizationDecision decision = new ApplicationRoleAuthorizationManager(repository, UserRole.DOCTOR)
                .check(() -> authentication, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void suspendedAccountCannotAccessItsRoleRoute() {
        UUID authUserId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(authUserId);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                user(authUserId, UserRole.PATIENT, AccountStatus.SUSPENDED)));

        AuthorizationDecision decision = new ApplicationRoleAuthorizationManager(repository, UserRole.PATIENT)
                .check(() -> authentication, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void bannedAccountCannotAccessItsNormalRoleRoute() {
        UUID authUserId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(authUserId);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                user(authUserId, UserRole.PATIENT, AccountStatus.BANNED)));

        AuthorizationDecision decision = new ApplicationRoleAuthorizationManager(repository, UserRole.PATIENT)
                .check(() -> authentication, context);

        assertThat(decision.isGranted()).isFalse();
    }

    private JwtAuthenticationToken authentication(UUID authUserId) {
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "RS256"), Map.of("sub", authUserId.toString()));
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_AUTHENTICATED")));
    }

    private AppUser user(UUID authUserId, UserRole role, AccountStatus status) {
        return new AppUser(authUserId, "person@example.com", "John", "Silva", null, role, status);
    }
}
