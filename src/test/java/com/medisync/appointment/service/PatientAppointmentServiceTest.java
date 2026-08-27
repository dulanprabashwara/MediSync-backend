package com.medisync.appointment.service;

import com.medisync.appointment.dto.CancelAppointmentRequest;
import com.medisync.appointment.dto.CreateAppointmentRequest;
import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.entity.AppointmentSymptoms;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.appointment.repository.AppointmentSymptomsRepository;
import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.DoctorAvailabilityWindow;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.availability.repository.DoctorAvailabilityWindowRepository;
import com.medisync.config.AppointmentProperties;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.consultation.service.ConsultationRealtimePublisher;
import com.medisync.consultation.service.VideoConsultationService;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAppointmentServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock PatientProfileRepository patientProfileRepository;
    @Mock AppointmentSlotRepository slotRepository;
    @Mock DoctorAvailabilityWindowRepository windowRepository;
    @Mock DoctorProfileRepository doctorProfileRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock HospitalRepository hospitalRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock SpecializationRepository specializationRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentSymptomsRepository symptomsRepository;
    @Mock AppointmentResponseMapper responseMapper;
    @Mock ConsultationSessionRepository consultationRepository;
    @Mock ConsultationRealtimePublisher realtimePublisher;
    @Mock com.medisync.notification.service.NotificationService notificationService;
    @Mock VideoConsultationService videoConsultationService;

    private PatientAppointmentService service;
    private Jwt jwt;
    private AppUser patientUser;
    private PatientProfile patient;
    private AppUser doctorUser;
    private DoctorProfile doctor;
    private DoctorAvailabilityWindow window;
    private AppointmentSlot slot;

    @BeforeEach
    void setUp() {
        service = new PatientAppointmentService(currentUserService, patientProfileRepository, slotRepository,
                windowRepository, doctorProfileRepository, appUserRepository, hospitalRepository,
                departmentRepository, specializationRepository, appointmentRepository, symptomsRepository,
                responseMapper, new AppointmentProperties(0), consultationRepository, realtimePublisher,
                Clock.systemUTC(), notificationService, videoConsultationService);
        UUID patientAuth = UUID.randomUUID();
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(patientAuth.toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        patientUser = new AppUser(patientAuth, "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        patient = new PatientProfile(patientUser.getId());
        doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        Hospital hospital = new Hospital("Central", null, "Colombo", null, true);
        Department department = new Department(hospital.getId(), "Medicine", true);
        Specialization specialization = new Specialization("Cardiology", null, true);
        doctor = new DoctorProfile(doctorUser.getId());
        doctor.updateProfessionalProfile("SLMC-1", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS", 8, null);
        doctor.verify(UUID.randomUUID());
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(3).withNano(0);
        window = new DoctorAvailabilityWindow(doctor.getId(), start, start.plusHours(1), 30, "Asia/Colombo");
        slot = new AppointmentSlot(window.getId(), doctor.getId(), start, start.plusMinutes(30));

        lenient().when(hospitalRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        lenient().when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        lenient().when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));
    }

    @Test
    void activePatientCreatesRequestedAppointmentSymptomsAndReservation() {
        allowBooking();
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(jwt, request());

        ArgumentCaptor<Appointment> appointment = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointment.capture());
        assertThat(appointment.getValue().getPatientId()).isEqualTo(patient.getId());
        assertThat(appointment.getValue().getDoctorId()).isEqualTo(doctor.getId());
        assertThat(appointment.getValue().getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
        ArgumentCaptor<AppointmentSymptoms> symptoms = ArgumentCaptor.forClass(AppointmentSymptoms.class);
        verify(symptomsRepository).save(symptoms.capture());
        assertThat(symptoms.getValue().getReasonForVisit()).isEqualTo("Persistent fever");
        verify(patientProfileRepository).findByUserIdForUpdate(patientUser.getId());
        verify(slotRepository).findByIdForUpdate(slot.getId());
    }

    @Test
    void wrongRoleCannotBookThroughPatientService() {
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE))
                .thenThrow(new AccessDeniedException("Wrong role"));
        assertThatThrownBy(() -> service.create(jwt, request())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pastSlotCannotBeBooked() {
        slot = new AppointmentSlot(window.getId(), doctor.getId(), OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30));
        allowPatientAndSlot();
        assertThatThrownBy(() -> service.create(jwt, request()))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("past");
    }

    @Test
    void staleLoadedSlotIsRejectedUnderLockAfterCrossingLeadTime() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
        slot = new AppointmentSlot(window.getId(), doctor.getId(), now.plusMinutes(5), now.plusMinutes(35));
        service = new PatientAppointmentService(currentUserService, patientProfileRepository, slotRepository,
                windowRepository, doctorProfileRepository, appUserRepository, hospitalRepository,
                departmentRepository, specializationRepository, appointmentRepository, symptomsRepository,
                responseMapper, new AppointmentProperties(10), consultationRepository, realtimePublisher,
                Clock.fixed(now.toInstant(), ZoneId.of("UTC")), notificationService, videoConsultationService);
        allowPatientAndSlot();

        assertThatThrownBy(() -> service.create(jwt, request()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("too close");
        verify(slotRepository).findByIdForUpdate(slot.getId());
    }

    @Test
    void blockedReservedAndBookedSlotsCannotBeBooked() {
        allowPatientAndSlot();
        slot.block();
        assertUnavailable();

        slot = futureSlot();
        slot.reserve();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        assertUnavailable();

        slot.book();
        assertUnavailable();
    }

    @Test
    void overlappingPatientAppointmentIsRejected() {
        allowBooking();
        when(appointmentRepository.existsActivePatientOverlap(patient.getId(), slot.getStartsAt(), slot.getEndsAt()))
                .thenReturn(true);
        assertThatThrownBy(() -> service.create(jwt, request()))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("overlapping");
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void patientCanCancelOwnRequestedAppointmentAndReleaseSlot() {
        slot.reserve();
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), slot.getId(),
                slot.getStartsAt(), slot.getEndsAt());
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(patientUser);
        when(patientProfileRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        when(appUserRepository.findById(patientUser.getId())).thenReturn(Optional.of(patientUser));

        service.cancel(jwt, appointment.getId(), new CancelAppointmentRequest("No longer available"));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_PATIENT);
        assertThat(appointment.getCancellationReason()).isEqualTo("No longer available");
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void patientCanCancelOwnConfirmedAppointmentAndReleaseSlot() {
        slot.reserve();
        slot.book();
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), slot.getId(),
                slot.getStartsAt(), slot.getEndsAt());
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(patientUser);
        when(patientProfileRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(consultationRepository.findByAppointmentIdForUpdate(appointment.getId()))
                .thenReturn(Optional.of(consultation));
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        when(appUserRepository.findById(patientUser.getId())).thenReturn(Optional.of(patientUser));

        service.cancel(jwt, appointment.getId(), null);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_PATIENT);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CANCELLED);
        verify(videoConsultationService).markSessionEnded(consultation.getId());
    }

    @Test
    void patientCannotCancelConsultationThatHasStarted() {
        slot.reserve();
        slot.book();
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), slot.getId(),
                slot.getStartsAt(), slot.getEndsAt());
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        consultation.start();
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(patientUser);
        when(patientProfileRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(consultationRepository.findByAppointmentIdForUpdate(appointment.getId()))
                .thenReturn(Optional.of(consultation));

        assertThatThrownBy(() -> service.cancel(jwt, appointment.getId(), null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("cannot be cancelled");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    void patientCannotCancelAnotherPatientsAppointment() {
        Appointment appointment = new Appointment(UUID.randomUUID(), doctor.getId(), slot.getId(),
                slot.getStartsAt(), slot.getEndsAt());
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(patientUser);
        when(patientProfileRepository.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(jwt, appointment.getId(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void allowBooking() {
        allowPatientAndSlot();
        when(windowRepository.findById(window.getId())).thenReturn(Optional.of(window));
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
    }

    private void allowPatientAndSlot() {
        when(currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE)).thenReturn(patientUser);
        when(patientProfileRepository.findByUserIdForUpdate(patientUser.getId())).thenReturn(Optional.of(patient));
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
    }

    private CreateAppointmentRequest request() {
        return new CreateAppointmentRequest(slot.getId(), 34, " Persistent fever ", "Fever and headache",
                "3 days", "Temperature was elevated");
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> service.create(jwt, request()))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("no longer available");
    }

    private AppointmentSlot futureSlot() {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(3).withNano(0);
        return new AppointmentSlot(window.getId(), doctor.getId(), start, start.plusMinutes(30));
    }
}
