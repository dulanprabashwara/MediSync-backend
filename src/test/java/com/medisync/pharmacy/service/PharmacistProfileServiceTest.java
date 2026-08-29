package com.medisync.pharmacy.service;

import com.medisync.exception.ResourceConflictException;
import com.medisync.pharmacy.dto.PharmacistProfileUpdateRequest;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.service.CurrentUserService;
import com.medisync.notification.service.ProfessionalVerificationNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PharmacistProfileServiceTest {

    private CurrentUserService currentUserService;
    private PharmacistProfileRepository repository;
    private PharmacistProfileService service;
    private ProfessionalVerificationNotificationService verificationNotifications;
    private AppUser user;
    private PharmacistProfile profile;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        repository = mock(PharmacistProfileRepository.class);
        verificationNotifications = mock(ProfessionalVerificationNotificationService.class);
        service = new PharmacistProfileService(currentUserService, repository, verificationNotifications);
        user = new AppUser(UUID.randomUUID(), "pharmacist@example.com", "Saman", "Perera", null,
                UserRole.PHARMACIST, AccountStatus.PENDING_VERIFICATION);
        profile = new PharmacistProfile(user.getId());
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(user.getAuthUserId().toString()).build();
        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(user);
        when(repository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(profile));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void pharmacistCanCompleteAndSubmitProfileWithoutControllingVerificationFields() {
        service.updateProfile(jwt, request());
        var response = service.submitForVerification(jwt);

        assertThat(response.profileComplete()).isTrue();
        assertThat(response.submitted()).isTrue();
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(profile.getVerifiedBy()).isNull();
    }

    @Test
    void rejectedPharmacistCanEditAndResubmit() {
        profile.updateProfessionalProfile("PH-1", "City Pharmacy", null, "1 Main Street", null);
        profile.submitForVerification();
        profile.reject("Correct the pharmacy address");

        service.updateProfile(jwt, request());
        service.submitForVerification(jwt);

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(profile.getVerificationRejectionReason()).isNull();
        assertThat(profile.isAwaitingReview()).isTrue();
    }

    @Test
    void submittedProfileCannotBeEditedDuringReview() {
        profile.updateProfessionalProfile("PH-1", "City Pharmacy", null, "1 Main Street", null);
        profile.submitForVerification();

        assertThatThrownBy(() -> service.updateProfile(jwt, request()))
                .isInstanceOf(ResourceConflictException.class);
    }

    private PharmacistProfileUpdateRequest request() {
        return new PharmacistProfileUpdateRequest("PH-1", "City Pharmacy", "REG-2",
                "2 Main Street, Colombo", "BPharm");
    }
}
