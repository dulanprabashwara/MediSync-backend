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
import com.medisync.user.repository.UserAccountBanRepository;
import com.medisync.media.MediaUrlService;
import com.medisync.user.dto.AccountStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository appUserRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PharmacistProfileRepository pharmacistProfileRepository;
    private final UserAccountBanRepository banRepository;
    private final MediaUrlService mediaUrlService;

    @Autowired
    public UserService(
            AppUserRepository appUserRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            PharmacistProfileRepository pharmacistProfileRepository,
            UserAccountBanRepository banRepository,
            MediaUrlService mediaUrlService
    ) {
        this.appUserRepository = appUserRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
        this.banRepository = banRepository;
        this.mediaUrlService = mediaUrlService;
    }

    UserService(AppUserRepository appUserRepository,
                PatientProfileRepository patientProfileRepository,
                DoctorProfileRepository doctorProfileRepository,
                PharmacistProfileRepository pharmacistProfileRepository) {
        this(appUserRepository, patientProfileRepository, doctorProfileRepository, pharmacistProfileRepository,
                null, null);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Jwt jwt) {
        UUID authUserId = authenticatedUserId(jwt);
        return appUserRepository.findByAuthUserId(authUserId)
                .map(user -> UserResponse.from(user, mediaUrlService == null
                        ? null : mediaUrlService.signedUrlOrNull(user.getProfileImageKey())))
                .orElseThrow(OnboardingRequiredException::new);
    }

    @Transactional(readOnly = true)
    public AccountStatusResponse getAccountStatus(Jwt jwt) {
        UUID authUserId = authenticatedUserId(jwt);
        AppUser user = appUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(OnboardingRequiredException::new);
        return banRepository.findByUserIdAndUnbannedAtIsNull(user.getId())
                .map(ban -> new AccountStatusResponse(user.getStatus(), ban.getReason(), ban.getBannedAt()))
                .orElseGet(() -> new AccountStatusResponse(user.getStatus(), null, null));
    }

    @Transactional
    public UserResponse updateSelfProfile(Jwt jwt, com.medisync.user.dto.UserProfileUpdateRequest request) {
        UUID authUserId = authenticatedUserId(jwt);
        AppUser user = appUserRepository.findByAuthUserId(authUserId)
                .orElseThrow(OnboardingRequiredException::new);

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(normalizePhone(request.phone()));

        AppUser savedUser = appUserRepository.saveAndFlush(user);
        return UserResponse.from(savedUser, mediaUrlService == null
                ? null : mediaUrlService.signedUrlOrNull(savedUser.getProfileImageKey()));
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
        return UserResponse.from(savedUser, null);
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
