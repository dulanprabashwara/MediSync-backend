package com.medisync.pharmacy.service;

import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.pharmacy.dto.AdminPharmacistReviewResponse;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.service.CurrentUserService;
import com.medisync.notification.service.ProfessionalVerificationNotificationService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminPharmacistVerificationService {

    private final CurrentUserService currentUserService;
    private final PharmacistProfileRepository pharmacistProfileRepository;
    private final AppUserRepository appUserRepository;
    private final ProfessionalVerificationNotificationService verificationNotifications;

    public AdminPharmacistVerificationService(CurrentUserService currentUserService,
                                              PharmacistProfileRepository pharmacistProfileRepository,
                                              AppUserRepository appUserRepository,
                                              ProfessionalVerificationNotificationService verificationNotifications) {
        this.currentUserService = currentUserService;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
        this.appUserRepository = appUserRepository;
        this.verificationNotifications = verificationNotifications;
    }

    @Transactional(readOnly = true)
    public List<AdminPharmacistReviewResponse> pendingPharmacists(Jwt jwt) {
        requireAdmin(jwt);
        return pharmacistProfileRepository
                .findByVerificationStatusAndSubmittedForVerificationAtIsNotNullOrderBySubmittedForVerificationAtAsc(
                        VerificationStatus.PENDING)
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public AdminPharmacistReviewResponse pharmacist(Jwt jwt, UUID pharmacistId) {
        requireAdmin(jwt);
        return response(pharmacistProfileRepository.findById(pharmacistId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist profile not found")));
    }

    @Transactional
    public AdminPharmacistReviewResponse verify(Jwt jwt, UUID pharmacistId) {
        AppUser admin = requireAdmin(jwt);
        PharmacistProfile profile = lockPendingSubmission(pharmacistId);
        if (!profile.isComplete()) {
            throw new InvalidRequestException("The pharmacist profile is incomplete");
        }
        AppUser pharmacist = appUserRepository.findByIdForUpdate(profile.getUserId())
                .filter(user -> user.getRole() == UserRole.PHARMACIST)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist account not found"));
        profile.verify(admin.getId());
        pharmacist.activate();
        pharmacistProfileRepository.saveAndFlush(profile);
        appUserRepository.saveAndFlush(pharmacist);
        verificationNotifications.reviewed(admin, pharmacist, profile.getId(), true,
                profile.getSubmittedForVerificationAt());
        return response(profile, pharmacist);
    }

    @Transactional
    public AdminPharmacistReviewResponse reject(Jwt jwt, UUID pharmacistId, String reason) {
        AppUser admin = requireAdmin(jwt);
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("A rejection reason is required");
        }
        PharmacistProfile profile = lockPendingSubmission(pharmacistId);
        profile.reject(reason.trim());
        PharmacistProfile saved = pharmacistProfileRepository.saveAndFlush(profile);
        AppUser pharmacist = appUserRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist account not found"));
        verificationNotifications.reviewed(admin, pharmacist, saved.getId(), false,
                saved.getSubmittedForVerificationAt());
        return response(saved, pharmacist);
    }

    private AppUser requireAdmin(Jwt jwt) {
        return currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE);
    }

    private PharmacistProfile lockPendingSubmission(UUID pharmacistId) {
        PharmacistProfile profile = pharmacistProfileRepository.findByIdForUpdate(pharmacistId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist profile not found"));
        if (!profile.isAwaitingReview()) {
            throw new ResourceConflictException("This verification submission has already been processed");
        }
        return profile;
    }

    private AdminPharmacistReviewResponse response(PharmacistProfile profile) {
        return response(profile, null);
    }

    private AdminPharmacistReviewResponse response(PharmacistProfile profile, AppUser knownUser) {
        AppUser user = knownUser == null ? appUserRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist account not found")) : knownUser;
        return new AdminPharmacistReviewResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), profile.getProfessionalRegistrationNumber(), profile.getPharmacyName(),
                profile.getPharmacyRegistrationNumber(), profile.getPharmacyAddress(), profile.getQualifications(),
                profile.getVerificationStatus(), profile.getVerificationRejectionReason(),
                profile.getSubmittedForVerificationAt(), profile.getVerifiedAt());
    }
}
