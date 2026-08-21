package com.medisync.pharmacy.service;

import com.medisync.exception.ResourceNotFoundException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class PharmacistAccessService {

    private final CurrentUserService currentUserService;
    private final PharmacistProfileRepository pharmacistProfileRepository;

    public PharmacistAccessService(CurrentUserService currentUserService,
                                   PharmacistProfileRepository pharmacistProfileRepository) {
        this.currentUserService = currentUserService;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
    }

    public PharmacistProfile requireVerifiedPharmacist(Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, UserRole.PHARMACIST, AccountStatus.ACTIVE);
        PharmacistProfile profile = pharmacistProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist profile not found"));
        if (profile.getVerificationStatus() != VerificationStatus.VERIFIED || !profile.isComplete()) {
            throw new AccessDeniedException("Only active verified pharmacists can use pharmacy operations");
        }
        return profile;
    }
}
