package com.medisync.appointment.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.DoctorProfileRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAppointmentServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock DoctorProfileRepository doctorProfileRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentSlotRepository slotRepository;
    @Mock AppointmentResponseMapper responseMapper;

    private DoctorAppointmentService service;
    private Jwt jwt;
    private AppUser doctorUser;
    private DoctorProfile doctor;
    private AppointmentSlot slot;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new DoctorAppointmentService(currentUserService, doctorProfileRepository, appointmentRepository,
                slotRepository, responseMapper);
        UUID authId = UUID.randomUUID();
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(authId.toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        doctorUser = new AppUser(authId, "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        doctor = new DoctorProfile(doctorUser.getId());
        doctor.verify(UUID.randomUUID());
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2).withNano(0);
        slot = new AppointmentSlot(UUID.randomUUID(), doctor.getId(), start, start.plusMinutes(30));
        slot.reserve();
        appointment = new Appointment(UUID.randomUUID(), doctor.getId(), slot.getId(), start, start.plusMinutes(30));
    }

    @Test
    void owningDoctorAcceptsRequestedAppointment() {
        allowDoctorAndLockedAppointment();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        service.accept(jwt, appointment.getId());

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getConfirmedAt()).isNotNull();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    void owningDoctorRejectsWithReasonAndReleasesSlot() {
        allowDoctorAndLockedAppointment();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        service.reject(jwt, appointment.getId(), " Please choose another time ");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(appointment.getDoctorRejectionReason()).isEqualTo("Please choose another time");
        assertThat(appointment.getRejectedAt()).isNotNull();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void anotherDoctorCannotAcceptOrRejectAppointment() {
        allowDoctor();
        Appointment otherAppointment = new Appointment(UUID.randomUUID(), UUID.randomUUID(), slot.getId(),
                slot.getStartsAt(), slot.getEndsAt());
        when(appointmentRepository.findByIdForUpdate(otherAppointment.getId()))
                .thenReturn(Optional.of(otherAppointment));

        assertThatThrownBy(() -> service.accept(jwt, otherAppointment.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.reject(jwt, otherAppointment.getId(), "Not mine"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmedAppointmentCannotBeRejected() {
        slot.book();
        appointment.confirm();
        allowDoctorAndLockedAppointment();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.reject(jwt, appointment.getId(), "Too late"))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void owningDoctorCancelsConfirmedAppointmentAndReleasesSlot() {
        slot.book();
        appointment.confirm();
        allowDoctorAndLockedAppointment();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        service.cancel(jwt, appointment.getId(), "Emergency leave");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_DOCTOR);
        assertThat(appointment.getCancellationReason()).isEqualTo("Emergency leave");
        assertThat(appointment.getCancelledAt()).isNotNull();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void nonVerifiedDoctorCannotManageAppointments() {
        DoctorProfile pending = new DoctorProfile(doctorUser.getId());
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE)).thenReturn(doctorUser);
        when(doctorProfileRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.accept(jwt, appointment.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void allowDoctorAndLockedAppointment() {
        allowDoctor();
        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
    }

    private void allowDoctor() {
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE)).thenReturn(doctorUser);
        when(doctorProfileRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
    }
}
