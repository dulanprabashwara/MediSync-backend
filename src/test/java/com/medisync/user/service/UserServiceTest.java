package com.medisync.user.service;

import com.medisync.user.dto.OnboardingRequest;
import com.medisync.user.dto.UserResponse;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.exception.DuplicateOnboardingException;
import com.medisync.user.exception.InvalidOnboardingRoleException;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private AppUserRepository appUserRepository;
    private PatientProfileRepository patientProfileRepository;
    private DoctorProfileRepository doctorProfileRepository;
    private PharmacistProfileRepository pharmacistProfileRepository;
    private UserService userService;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        patientProfileRepository = mock(PatientProfileRepository.class);
        doctorProfileRepository = mock(DoctorProfileRepository.class);
        pharmacistProfileRepository = mock(PharmacistProfileRepository.class);
        userService = new UserService(
                appUserRepository,
                patientProfileRepository,
                doctorProfileRepository,
                pharmacistProfileRepository
        );
        jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("email", "Person@Example.com")
                .build();
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void patientOnboardingCreatesActivePatientProfile() {
        UserResponse response = userService.onboard(jwt, request(UserRole.PATIENT));

        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.professionalVerificationStatus()).isNull();
        assertThat(response.email()).isEqualTo("person@example.com");
        verify(patientProfileRepository).save(any());
        verify(doctorProfileRepository, never()).save(any());
        verify(pharmacistProfileRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DOCTOR", "PHARMACIST"})
    void professionalOnboardingCreatesPendingProfile(UserRole role) {
        UserResponse response = userService.onboard(jwt, request(role));

        assertThat(response.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(response.professionalVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        if (role == UserRole.DOCTOR) {
            verify(doctorProfileRepository).save(any());
        } else {
            verify(pharmacistProfileRepository).save(any());
        }
    }

    @Test
    void adminCannotBeSelectedDuringPublicOnboarding() {
        assertThatThrownBy(() -> userService.onboard(jwt, request(UserRole.ADMIN)))
                .isInstanceOf(InvalidOnboardingRoleException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateOnboardingIsRejected() {
        when(appUserRepository.existsByAuthUserId(any())).thenReturn(true);

        assertThatThrownBy(() -> userService.onboard(jwt, request(UserRole.PATIENT)))
                .isInstanceOf(DuplicateOnboardingException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    private OnboardingRequest request(UserRole role) {
        return new OnboardingRequest("John", "Silva", "0712345678", role);
    }
}
