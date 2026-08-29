package com.medisync.doctor.service;

import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.config.AppointmentProperties;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.doctor.dto.DoctorSummaryResponse;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class PatientDoctorDiscoveryServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock DoctorProfileRepository doctorProfileRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock HospitalRepository hospitalRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock SpecializationRepository specializationRepository;
    @Mock AppointmentSlotRepository slotRepository;

    private PatientDoctorDiscoveryService service;
    private Jwt jwt;
    private AppUser patient;
    private AppUser doctorUser;
    private DoctorProfile doctor;
    private Hospital hospital;
    private Department department;
    private Specialization specialization;

    @BeforeEach
    void setUp() {
        service = new PatientDoctorDiscoveryService(currentUserService, doctorProfileRepository, appUserRepository,
                hospitalRepository, departmentRepository, specializationRepository, slotRepository,
                new AppointmentProperties(0), Clock.systemUTC());
        UUID authId = UUID.randomUUID();
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(authId.toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        patient = new AppUser(authId, "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        doctorUser = new AppUser(UUID.randomUUID(), "private@example.com", "Asha", "Perera", "private-phone",
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        hospital = new Hospital("Central Hospital", null, "Colombo", null, true);
        department = new Department(hospital.getId(), "Heart Centre", true);
        specialization = new Specialization("Cardiology", null, true);
        doctor = new DoctorProfile(doctorUser.getId());
        doctor.updateProfessionalProfile("SLMC-10", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS, MD", 12, "Experienced cardiologist");
        doctor.verify(UUID.randomUUID());
    }

    @Test
    void activeVerifiedDoctorAppearsWithPublicPhoneButWithoutPrivateIdentityFields() {
        allowPatient();
        when(doctorProfileRepository.searchDiscoverable(null, null, null, null, Pageable.ofSize(10)))
                .thenReturn(new PageImpl<>(java.util.List.of(doctor), Pageable.ofSize(10), 1));
        allowMapping();

        var result = service.search(jwt, null, null, null, null, 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).displayName()).isEqualTo("Dr. Asha Perera");
        assertThat(result.content().get(0).phone()).isEqualTo("private-phone");
        assertThat(Arrays.stream(DoctorSummaryResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("email", "userId", "authUserId", "verifiedBy");
    }

    @Test
    void searchPassesNameAndAllProfessionalFiltersWithPagination() {
        allowPatient();
        when(doctorProfileRepository.searchDiscoverable(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.search(jwt, " Asha ", hospital.getId(), department.getId(), specialization.getId(), 2, 7);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(doctorProfileRepository).searchDiscoverable(eq("Asha"), eq(hospital.getId()), eq(department.getId()),
                eq(specialization.getId()), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(7);
    }

    @Test
    void pendingAndRejectedDoctorsCannotBeOpened() {
        allowPatient();
        DoctorProfile pending = new DoctorProfile(doctorUser.getId());
        when(doctorProfileRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        assertThatThrownBy(() -> service.details(jwt, pending.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        DoctorProfile rejected = new DoctorProfile(doctorUser.getId());
        rejected.reject("Invalid registration");
        when(doctorProfileRepository.findById(rejected.getId())).thenReturn(Optional.of(rejected));
        assertThatThrownBy(() -> service.details(jwt, rejected.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void suspendedDoctorCannotBeOpened() {
        allowPatient();
        AppUser suspended = new AppUser(UUID.randomUUID(), "doctor@example.com", "Asha", "Perera", null,
                UserRole.DOCTOR, AccountStatus.SUSPENDED);
        DoctorProfile suspendedProfile = new DoctorProfile(suspended.getId());
        suspendedProfile.verify(UUID.randomUUID());
        when(doctorProfileRepository.findById(suspendedProfile.getId())).thenReturn(Optional.of(suspendedProfile));
        when(appUserRepository.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> service.details(jwt, suspendedProfile.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inactiveMasterDataExcludesDoctorFromDetail() {
        allowPatient();
        Hospital inactiveHospital = new Hospital("Closed Hospital", null, null, null, false);
        DoctorProfile inactiveReferenceDoctor = new DoctorProfile(doctorUser.getId());
        inactiveReferenceDoctor.updateProfessionalProfile("SLMC-20", inactiveHospital.getId(), department.getId(),
                specialization.getId(), "MBBS", 5, null);
        inactiveReferenceDoctor.verify(UUID.randomUUID());
        when(doctorProfileRepository.findById(inactiveReferenceDoctor.getId()))
                .thenReturn(Optional.of(inactiveReferenceDoctor));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        when(hospitalRepository.findById(inactiveHospital.getId())).thenReturn(Optional.of(inactiveHospital));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));

        assertThatThrownBy(() -> service.details(jwt, inactiveReferenceDoctor.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void patientSlotQueryUsesStrictServerLeadTimeBoundary() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
        service = new PatientDoctorDiscoveryService(currentUserService, doctorProfileRepository, appUserRepository,
                hospitalRepository, departmentRepository, specializationRepository, slotRepository,
                new AppointmentProperties(15), Clock.fixed(now.toInstant(), ZoneId.of("UTC")));
        allowPatient();
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        allowMapping();
        OffsetDateTime earliest = now.plusMinutes(15);
        OffsetDateTime rangeEnd = LocalDate.of(2026, 8, 22).plusDays(1)
                .atStartOfDay(ZoneId.of("Asia/Colombo")).toOffsetDateTime();
        when(slotRepository.findVisibleAvailableSlots(doctor.getId(), earliest, rangeEnd))
                .thenReturn(java.util.List.of());

        service.availableSlots(jwt, doctor.getId(), LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 8, 22));

        verify(slotRepository).findVisibleAvailableSlots(doctor.getId(), earliest, rangeEnd);
    }

    private void allowPatient() {
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(patient);
    }

    private void allowMapping() {
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        when(hospitalRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));
    }
}
