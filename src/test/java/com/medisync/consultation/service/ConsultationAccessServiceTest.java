package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationAccessServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock ConsultationSessionRepository consultationRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientProfileRepository patientProfileRepository;
    @Mock DoctorProfileRepository doctorProfileRepository;
    @Mock AppUserRepository appUserRepository;

    private ConsultationAccessService service;
    private Jwt jwt;
    private AppUser patientUser;
    private AppUser doctorUser;
    private PatientProfile patient;
    private DoctorProfile doctor;
    private Appointment appointment;
    private ConsultationSession consultation;

    @BeforeEach
    void setUp() {
        service = new ConsultationAccessService(currentUserService, consultationRepository, appointmentRepository,
                patientProfileRepository, doctorProfileRepository, appUserRepository);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        patient = new PatientProfile(patientUser.getId());
        doctor = new DoctorProfile(doctorUser.getId());
        doctor.verify(UUID.randomUUID());
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(), start, start.plusMinutes(30));
        appointment.confirm();
        consultation = new ConsultationSession(appointment.getId());
    }

    @Test
    void patientCannotAccessAnotherPatientsConsultationOrCancellationReason() {
        AppUser otherUser = new AppUser(UUID.randomUUID(), "other@example.com", "Other", "Patient", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        PatientProfile otherPatient = new PatientProfile(otherUser.getId());
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(otherUser);
        when(patientProfileRepository.findByUserId(otherUser.getId())).thenReturn(Optional.of(otherPatient));
        allowStoredContext();

        assertThatThrownBy(() -> service.requirePatient(jwt, consultation.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void doctorCannotAccessAnotherDoctorsConsultationOrCancellationReason() {
        AppUser otherUser = new AppUser(UUID.randomUUID(), "other-doctor@example.com", "Other", "Doctor", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        DoctorProfile otherDoctor = new DoctorProfile(otherUser.getId());
        otherDoctor.verify(UUID.randomUUID());
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE)).thenReturn(otherUser);
        when(doctorProfileRepository.findByUserId(otherUser.getId())).thenReturn(Optional.of(otherDoctor));
        allowStoredContext();

        assertThatThrownBy(() -> service.requireDoctor(jwt, consultation.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void allowStoredContext() {
        when(consultationRepository.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appUserRepository.findById(patientUser.getId())).thenReturn(Optional.of(patientUser));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
    }
}
