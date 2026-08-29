package com.medisync.doctor.service;

import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.doctor.dto.DoctorProfileResponse;
import com.medisync.doctor.dto.DoctorProfileUpdateRequest;
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
import com.medisync.user.repository.DoctorProfileRepository;
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

class DoctorProfileServiceTest {

    private CurrentUserService currentUserService;
    private DoctorProfileRepository doctorProfileRepository;
    private HospitalRepository hospitalRepository;
    private DepartmentRepository departmentRepository;
    private SpecializationRepository specializationRepository;
    private ProfessionalVerificationNotificationService verificationNotifications;
    private DoctorProfileService service;
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
        hospitalRepository = mock(HospitalRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        specializationRepository = mock(SpecializationRepository.class);
        verificationNotifications = mock(ProfessionalVerificationNotificationService.class);
        service = new DoctorProfileService(currentUserService, doctorProfileRepository, hospitalRepository,
                departmentRepository, specializationRepository, verificationNotifications);

        doctor = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION);
        profile = new DoctorProfile(doctor.getId());
        hospital = new Hospital("Central Hospital", null, "Colombo", null, true);
        department = new Department(hospital.getId(), "Cardiology", true);
        specialization = new Specialization("Cardiology", null, true);
        jwt = Jwt.withTokenValue("token").header("alg", "ES256")
                .subject(doctor.getAuthUserId().toString()).build();

        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(doctor);
        when(doctorProfileRepository.findByUserIdForUpdate(doctor.getId())).thenReturn(Optional.of(profile));
        when(doctorProfileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(hospitalRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));
    }

    @Test
    void departmentMustBelongToSelectedHospital() {
        Hospital anotherHospital = new Hospital("Other Hospital", null, null, null, true);
        Department wrongDepartment = new Department(anotherHospital.getId(), "Neurology", true);
        when(departmentRepository.findById(wrongDepartment.getId())).thenReturn(Optional.of(wrongDepartment));

        assertThatThrownBy(() -> service.updateProfile(jwt, request(hospital.getId(), wrongDepartment.getId())))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void incompleteProfileCannotBeSubmitted() {
        assertThatThrownBy(() -> service.submitForVerification(jwt))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("required professional fields");
    }

    @Test
    void completeProfileCanBeSubmitted() {
        profile.updateProfessionalProfile("SLMC-123", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS, MD", 8, "Cardiologist");

        DoctorProfileResponse response = service.submitForVerification(jwt);

        assertThat(response.submitted()).isTrue();
        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.submittedForVerificationAt()).isNotNull();
    }

    @Test
    void rejectedDoctorCanResubmit() {
        profile.updateProfessionalProfile("SLMC-123", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS", 3, null);
        profile.submitForVerification();
        profile.reject("Provide clearer qualifications");

        DoctorProfileResponse response = service.submitForVerification(jwt);

        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.verificationRejectionReason()).isNull();
        assertThat(response.submitted()).isTrue();
    }

    @Test
    void sensitiveVerifiedFieldsCannotBeChanged() {
        profile.updateProfessionalProfile("SLMC-123", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS", 3, null);
        profile.submitForVerification();
        profile.verify(UUID.randomUUID());
        DoctorProfileUpdateRequest update = new DoctorProfileUpdateRequest("SLMC-999", null, null, null,
                null, null, "Updated bio", null, null, null, null);

        assertThatThrownBy(() -> service.updateProfile(jwt, update))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("locked");
    }

    private DoctorProfileUpdateRequest request(UUID hospitalId, UUID departmentId) {
        return new DoctorProfileUpdateRequest("SLMC-123", hospitalId, departmentId, specialization.getId(),
                "MBBS", 4, null, null, null, null, null);
    }
}
