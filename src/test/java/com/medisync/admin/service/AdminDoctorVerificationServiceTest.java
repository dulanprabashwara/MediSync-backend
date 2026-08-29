package com.medisync.admin.service;

import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import com.medisync.notification.service.ProfessionalVerificationNotificationService;
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

class AdminDoctorVerificationServiceTest {

    private CurrentUserService currentUserService;
    private DoctorProfileRepository doctorProfileRepository;
    private AppUserRepository appUserRepository;
    private HospitalRepository hospitalRepository;
    private DepartmentRepository departmentRepository;
    private SpecializationRepository specializationRepository;
    private ProfessionalVerificationNotificationService verificationNotifications;
    private AdminDoctorVerificationService service;
    private AppUser admin;
    private AppUser doctor;
    private DoctorProfile profile;
    private Hospital hospital;
    private Department department;
    private Specialization specialization;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        doctorProfileRepository = mock(DoctorProfileRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        specializationRepository = mock(SpecializationRepository.class);
        verificationNotifications = mock(ProfessionalVerificationNotificationService.class);
        service = new AdminDoctorVerificationService(currentUserService, doctorProfileRepository, appUserRepository,
                hospitalRepository, departmentRepository, specializationRepository, verificationNotifications);

        admin = new AppUser(UUID.randomUUID(), "admin@example.com", "Admin", "User", null,
                UserRole.ADMIN, AccountStatus.ACTIVE);
        doctor = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION);
        profile = new DoctorProfile(doctor.getId());
        hospital = new Hospital("Central Hospital", null, null, null, true);
        department = new Department(hospital.getId(), "Cardiology", true);
        specialization = new Specialization("Cardiology", null, true);
        profile.updateProfessionalProfile("SLMC-123", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS", 5, null);
        profile.submitForVerification();
        jwt = Jwt.withTokenValue("token").header("alg", "ES256")
                .subject(admin.getAuthUserId().toString()).build();

        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(admin);
        when(doctorProfileRepository.findByIdForUpdate(profile.getId())).thenReturn(Optional.of(profile));
        when(appUserRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appUserRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));
        when(doctorProfileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(appUserRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void verificationMarksProfileVerifiedAndActivatesDoctor() {
        service.verify(jwt, profile.getId());

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(profile.getVerifiedBy()).isEqualTo(admin.getId());
        assertThat(profile.getVerifiedAt()).isNotNull();
        assertThat(doctor.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void rejectionRequiresReason() {
        assertThatThrownBy(() -> service.reject(jwt, profile.getId(), " "))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectedProfileStoresReason() {
        service.reject(jwt, profile.getId(), "Registration could not be confirmed");

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(profile.getVerificationRejectionReason()).contains("could not be confirmed");
        assertThat(doctor.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    void processedSubmissionCannotBeOverwritten() {
        profile.verify(admin.getId());

        assertThatThrownBy(() -> service.reject(jwt, profile.getId(), "Change it"))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void doctorCannotSelfVerifyAtServiceBoundary() {
        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class)))
                .thenThrow(new AccessDeniedException("forbidden"));

        assertThatThrownBy(() -> service.verify(jwt, profile.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(doctorProfileRepository, never()).findByIdForUpdate(any());
    }
}
