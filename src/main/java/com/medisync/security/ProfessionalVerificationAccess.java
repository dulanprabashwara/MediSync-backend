package com.medisync.security;

import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalVerificationAccess {

    private final DoctorProfileRepository doctorProfileRepository;
    private final PharmacistProfileRepository pharmacistProfileRepository;

    public ProfessionalVerificationAccess(DoctorProfileRepository doctorProfileRepository,
                                          PharmacistProfileRepository pharmacistProfileRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
    }

    public boolean isVerified(AppUser user) {
        if (user.getRole() == UserRole.DOCTOR) {
            return doctorProfileRepository.findByUserId(user.getId())
                    .map(profile -> profile.getVerificationStatus() == VerificationStatus.VERIFIED)
                    .orElse(false);
        }
        if (user.getRole() == UserRole.PHARMACIST) {
            return pharmacistProfileRepository.findByUserId(user.getId())
                    .map(profile -> profile.getVerificationStatus() == VerificationStatus.VERIFIED)
                    .orElse(false);
        }
        return true;
    }
}
