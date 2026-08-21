package com.medisync.pharmacy.service;

import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.pharmacy.dto.PharmacistProfileResponse;
import com.medisync.pharmacy.dto.PharmacistProfileUpdateRequest;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PharmacistProfileService {

    private final CurrentUserService currentUserService;
    private final PharmacistProfileRepository pharmacistProfileRepository;

    public PharmacistProfileService(CurrentUserService currentUserService,
                                    PharmacistProfileRepository pharmacistProfileRepository) {
        this.currentUserService = currentUserService;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
    }

    @Transactional(readOnly = true)
    public PharmacistProfileResponse getProfile(Jwt jwt) {
        AppUser user = requirePharmacist(jwt);
        return response(requireProfile(user), user);
    }

    @Transactional
    public PharmacistProfileResponse updateProfile(Jwt jwt, PharmacistProfileUpdateRequest request) {
        AppUser user = requirePharmacist(jwt);
        PharmacistProfile profile = pharmacistProfileRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist profile not found"));
        if (profile.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new ResourceConflictException("Verified professional identity fields are locked");
        }
        if (profile.isAwaitingReview()) {
            throw new ResourceConflictException("A submitted profile cannot be changed while it is awaiting review");
        }

        String registration = optional(request.professionalRegistrationNumber());
        if (registration != null
                && pharmacistProfileRepository.existsByProfessionalRegistrationNumberIgnoreCaseAndIdNot(
                registration, profile.getId())) {
            throw new ResourceConflictException("This professional registration number is already in use");
        }
        profile.updateProfessionalProfile(registration, optional(request.pharmacyName()),
                optional(request.pharmacyRegistrationNumber()), optional(request.pharmacyAddress()),
                optional(request.qualifications()));
        return response(pharmacistProfileRepository.saveAndFlush(profile), user);
    }

    @Transactional
    public PharmacistProfileResponse submitForVerification(Jwt jwt) {
        AppUser user = requirePharmacist(jwt);
        PharmacistProfile profile = pharmacistProfileRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist profile not found"));
        if (profile.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new ResourceConflictException("This pharmacist is already verified");
        }
        if (profile.isAwaitingReview()) {
            throw new ResourceConflictException("This profile is already awaiting verification");
        }
        if (!profile.isComplete()) {
            throw new InvalidRequestException("Complete the registration number, pharmacy name, and pharmacy address before submitting");
        }
        profile.submitForVerification();
        return response(pharmacistProfileRepository.saveAndFlush(profile), user);
    }

    private AppUser requirePharmacist(Jwt jwt) {
        return currentUserService.requireRole(jwt, UserRole.PHARMACIST,
                AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE);
    }

    private PharmacistProfile requireProfile(AppUser user) {
        return pharmacistProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist profile not found"));
    }

    private PharmacistProfileResponse response(PharmacistProfile profile, AppUser knownUser) {
        AppUser user = knownUser;
        boolean active = user != null && user.getStatus() == AccountStatus.ACTIVE;
        return new PharmacistProfileResponse(profile.getId(), profile.getProfessionalRegistrationNumber(),
                profile.getPharmacyName(), profile.getPharmacyRegistrationNumber(), profile.getPharmacyAddress(),
                profile.getQualifications(), profile.getVerificationStatus(), profile.getVerificationRejectionReason(),
                profile.getSubmittedForVerificationAt(), profile.getVerifiedAt(), profile.isComplete(),
                profile.isAwaitingReview(), profile.getVerificationStatus() != VerificationStatus.VERIFIED
                && !profile.isAwaitingReview(), active && profile.getVerificationStatus() == VerificationStatus.VERIFIED);
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
