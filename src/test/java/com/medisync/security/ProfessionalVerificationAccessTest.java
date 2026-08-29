package com.medisync.security;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfessionalVerificationAccessTest {

    private DoctorProfileRepository doctorProfiles;
    private PharmacistProfileRepository pharmacistProfiles;
    private ProfessionalVerificationAccess access;

    @BeforeEach
    void setUp() {
        doctorProfiles = mock(DoctorProfileRepository.class);
        pharmacistProfiles = mock(PharmacistProfileRepository.class);
        access = new ProfessionalVerificationAccess(doctorProfiles, pharmacistProfiles);
    }

    @Test
    void missingDoctorProfileIsNotVerified() {
        AppUser doctor = professional(UserRole.DOCTOR);
        when(doctorProfiles.findByUserId(doctor.getId())).thenReturn(Optional.empty());

        assertThat(access.isVerified(doctor)).isFalse();
    }

    @Test
    void pendingDoctorProfileIsNotVerified() {
        AppUser doctor = professional(UserRole.DOCTOR);
        DoctorProfile profile = new DoctorProfile(doctor.getId());
        when(doctorProfiles.findByUserId(doctor.getId())).thenReturn(Optional.of(profile));

        assertThat(access.isVerified(doctor)).isFalse();
    }

    @Test
    void rejectedDoctorProfileIsNotVerified() {
        AppUser doctor = professional(UserRole.DOCTOR);
        DoctorProfile profile = new DoctorProfile(doctor.getId());
        profile.reject("Registration could not be validated");
        when(doctorProfiles.findByUserId(doctor.getId())).thenReturn(Optional.of(profile));

        assertThat(access.isVerified(doctor)).isFalse();
    }

    @Test
    void verifiedDoctorProfileIsVerified() {
        AppUser doctor = professional(UserRole.DOCTOR);
        DoctorProfile profile = new DoctorProfile(doctor.getId());
        profile.verify(UUID.randomUUID());
        when(doctorProfiles.findByUserId(doctor.getId())).thenReturn(Optional.of(profile));

        assertThat(access.isVerified(doctor)).isTrue();
    }

    @Test
    void missingPendingAndRejectedPharmacistProfilesAreNotVerified() {
        AppUser pharmacist = professional(UserRole.PHARMACIST);
        assertThat(access.isVerified(pharmacist)).isFalse();

        PharmacistProfile pending = new PharmacistProfile(pharmacist.getId());
        when(pharmacistProfiles.findByUserId(pharmacist.getId())).thenReturn(Optional.of(pending));
        assertThat(access.isVerified(pharmacist)).isFalse();

        pending.reject("License could not be validated");
        assertThat(access.isVerified(pharmacist)).isFalse();
    }

    @Test
    void verifiedPharmacistProfileIsVerified() {
        AppUser pharmacist = professional(UserRole.PHARMACIST);
        PharmacistProfile profile = new PharmacistProfile(pharmacist.getId());
        profile.verify(UUID.randomUUID());
        when(pharmacistProfiles.findByUserId(pharmacist.getId())).thenReturn(Optional.of(profile));

        assertThat(access.isVerified(pharmacist)).isTrue();
    }

    private AppUser professional(UserRole role) {
        return new AppUser(UUID.randomUUID(), role.name().toLowerCase() + "@example.com",
                "Test", role.name(), null, role, AccountStatus.ACTIVE);
    }
}
