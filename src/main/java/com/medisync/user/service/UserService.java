package com.medisync.user.service;

import com.medisync.user.dto.OnboardingRequest;
import com.medisync.user.dto.UserResponse;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.exception.DuplicateOnboardingException;
import com.medisync.user.exception.InvalidAuthenticatedUserException;
import com.medisync.user.exception.InvalidOnboardingRoleException;
import com.medisync.user.exception.OnboardingRequiredException;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository appUserRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PharmacistProfileRepository pharmacistProfileRepository;

    public UserService(
            AppUserRepository appUserRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            PharmacistProfileRepository pharmacistProfileRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Jwt jwt) {
        UUID authUserId = authenticatedUserId(jwt);
        return appUserRepository.findByAuthUserId(authUserId)
                .map(UserResponse::from)
                .orElseThrow(OnboardingRequiredException::new);
    }

    @Transactional
    public UserResponse onboard(Jwt jwt, OnboardingRequest request) {
        UUID authUserId = authenticatedUserId(jwt);
        String email = authenticatedEmail(jwt);

        if (request.role() == UserRole.ADMIN) {
            throw new InvalidOnboardingRoleException();
        }
        if (appUserRepository.existsByAuthUserId(authUserId)) {
            throw new DuplicateOnboardingException();
        }

        AccountStatus status = request.role() == UserRole.PATIENT
                ? AccountStatus.ACTIVE
                : AccountStatus.PENDING_VERIFICATION;

        AppUser user = new AppUser(
                authUserId,
                email,
                request.firstName().trim(),
                request.lastName().trim(),
                normalizePhone(request.phone()),
                request.role(),
                status
        );
        AppUser savedUser = appUserRepository.saveAndFlush(user);
        createRoleProfile(savedUser);

        log.info("User onboarding completed for role {}", savedUser.getRole());
        return UserResponse.from(savedUser);
    }

    private void createRoleProfile(AppUser user) {
        switch (user.getRole()) {
            case PATIENT -> patientProfileRepository.save(new PatientProfile(user.getId()));
            case DOCTOR -> doctorProfileRepository.save(new DoctorProfile(user.getId()));
            case PHARMACIST -> pharmacistProfileRepository.save(new PharmacistProfile(user.getId()));
            case ADMIN -> throw new InvalidOnboardingRoleException();
        }
    }

    private UUID authenticatedUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidAuthenticatedUserException("The authenticated user identifier is invalid");
        }
    }

    private String authenticatedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank() || email.length() > 320) {
            throw new InvalidAuthenticatedUserException("The authenticated account does not contain a valid email address");
        }
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
