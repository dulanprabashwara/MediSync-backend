package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.entity.ConsultationVideoSession;
import com.medisync.consultation.repository.ConsultationVideoSessionRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.notification.service.NotificationService;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class VideoConsultationServiceTest {

    @Mock ConsultationAccessService accessService;
    @Mock LiveKitTokenService liveKitTokenService;
    @Mock NotificationService notificationService;
    @Mock ConsultationVideoSessionRepository videoSessionRepository;

    private VideoConsultationService service;
    private Jwt jwt;
    private ConsultationAccessService.ConsultationContext context;

    @BeforeEach
    void setUp() {
        service = new VideoConsultationService(accessService, liveKitTokenService, notificationService, videoSessionRepository);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        context = createContext(ConsultationStatus.IN_PROGRESS, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
    }

    @Test
    void doctorStartVideo_createsNewSession() {
        when(accessService.requireDoctor(jwt, context.consultation().getId(), false)).thenReturn(context);
        when(videoSessionRepository.findByConsultationId(context.consultation().getId())).thenReturn(Optional.empty());
        when(liveKitTokenService.generateToken(anyString(), any(UUID.class), anyString(), anyBoolean())).thenReturn("lk-token");

        var response = service.doctorStartVideo(jwt, context.consultation().getId());

        assertThat(response.token()).isEqualTo("lk-token");
        
        ArgumentCaptor<ConsultationVideoSession> sessionCaptor = ArgumentCaptor.forClass(ConsultationVideoSession.class);
        verify(videoSessionRepository).saveAndFlush(sessionCaptor.capture());
        
        ConsultationVideoSession saved = sessionCaptor.getValue();
        assertThat(saved.getProviderRoomName()).startsWith("medisync-v-");
        assertThat(saved.getStatus()).isEqualTo(ConsultationVideoSession.VideoSessionStatus.ACTIVE);
        
        verify(notificationService).createNotification(
                eq(context.patientUser().getId()),
                eq(context.doctorUser().getId()),
                any(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                eq(context.consultation().getId()),
                anyString()
        );
    }

    @Test
    void doctorStartVideo_concurrency_doubleStartThrowsConflictException() {
        when(accessService.requireDoctor(jwt, context.consultation().getId(), false)).thenReturn(context);
        when(videoSessionRepository.findByConsultationId(context.consultation().getId())).thenReturn(Optional.empty());
        
        // Simulate a race condition where another thread already created the room and committed, causing unique constraint violation
        when(videoSessionRepository.saveAndFlush(any(ConsultationVideoSession.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation on consultation_id"));

        assertThatThrownBy(() -> service.doctorStartVideo(jwt, context.consultation().getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void patientJoinVideo_failsIfSessionNotActive() {
        when(accessService.requirePatient(jwt, context.consultation().getId(), false)).thenReturn(context);
        when(videoSessionRepository.findByConsultationId(context.consultation().getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patientJoinVideo(jwt, context.consultation().getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("not started");
    }

    @Test
    void markSessionEnded_updatesStatusToEnded() {
        ConsultationVideoSession session = new ConsultationVideoSession(context.consultation().getId(), "room", context.doctorUser().getId());
        when(videoSessionRepository.findByConsultationId(context.consultation().getId()))
                .thenReturn(Optional.of(session));

        service.markSessionEnded(context.consultation().getId());

        assertThat(session.getStatus()).isEqualTo(ConsultationVideoSession.VideoSessionStatus.ENDED);
        verify(videoSessionRepository).save(session);
    }
    
    @Test
    void markSessionEnded_doesNothingIfSessionDoesNotExist() {
        when(videoSessionRepository.findByConsultationId(context.consultation().getId()))
                .thenReturn(Optional.empty());

        service.markSessionEnded(context.consultation().getId());
        
        verify(videoSessionRepository, never()).save(any());
    }

    private ConsultationAccessService.ConsultationContext createContext(ConsultationStatus status, OffsetDateTime scheduledStart) {
        AppUser patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        AppUser doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        PatientProfile patient = new PatientProfile(patientUser.getId());
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());
        
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(), scheduledStart,
                scheduledStart.plusMinutes(30));
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        if (status == ConsultationStatus.IN_PROGRESS) {
            consultation.start();
        } else if (status == ConsultationStatus.COMPLETED) {
            consultation.start();
            consultation.complete();
        } else if (status == ConsultationStatus.CANCELLED) {
            consultation.cancel();
        }
        return new ConsultationAccessService.ConsultationContext(
                consultation, appointment, patient, doctor, patientUser, doctorUser);
    }
}
