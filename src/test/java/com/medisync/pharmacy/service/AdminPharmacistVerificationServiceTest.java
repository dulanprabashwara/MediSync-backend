package com.medisync.pharmacy.service;

import com.medisync.exception.InvalidRequestException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPharmacistVerificationServiceTest {

    private CurrentUserService currentUserService;
    private PharmacistProfileRepository profileRepository;
    private AppUserRepository userRepository;
    private AdminPharmacistVerificationService service;
    private AppUser admin;
    private AppUser pharmacist;
    private PharmacistProfile profile;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        profileRepository = mock(PharmacistProfileRepository.class);
        userRepository = mock(AppUserRepository.class);
        service = new AdminPharmacistVerificationService(currentUserService, profileRepository, userRepository);
        admin = new AppUser(UUID.randomUUID(), "admin@example.com", "Admin", "User", null,
                UserRole.ADMIN, AccountStatus.ACTIVE);
        pharmacist = new AppUser(UUID.randomUUID(), "pharmacist@example.com", "Saman", "Perera", null,
                UserRole.PHARMACIST, AccountStatus.PENDING_VERIFICATION);
        profile = new PharmacistProfile(pharmacist.getId());
        profile.updateProfessionalProfile("PH-1", "City Pharmacy", null, "1 Main Street", "BPharm");
        profile.submitForVerification();
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(admin.getAuthUserId().toString()).build();
        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(admin);
        when(profileRepository.findByIdForUpdate(profile.getId())).thenReturn(Optional.of(profile));
        when(userRepository.findByIdForUpdate(pharmacist.getId())).thenReturn(Optional.of(pharmacist));
        when(userRepository.findById(pharmacist.getId())).thenReturn(Optional.of(pharmacist));
        when(profileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvalVerifiesProfileAndActivatesAccountInOneServiceOperation() {
        service.verify(jwt, profile.getId());

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(profile.getVerifiedBy()).isEqualTo(admin.getId());
        assertThat(pharmacist.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void rejectionRequiresAndStoresReasonWithoutActivation() {
        assertThatThrownBy(() -> service.reject(jwt, profile.getId(), " "))
                .isInstanceOf(InvalidRequestException.class);

        service.reject(jwt, profile.getId(), "Registration could not be confirmed");
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(profile.getVerificationRejectionReason()).contains("could not be confirmed");
        assertThat(pharmacist.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    void pharmacistCannotSelfVerifyAtServiceBoundary() {
        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class)))
                .thenThrow(new AccessDeniedException("forbidden"));

        assertThatThrownBy(() -> service.verify(jwt, profile.getId())).isInstanceOf(AccessDeniedException.class);
        verify(profileRepository, never()).findByIdForUpdate(any());
    }
}
