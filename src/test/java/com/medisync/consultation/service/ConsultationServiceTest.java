package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.consultation.dto.UpdateClinicalNoteRequest;
import com.medisync.consultation.entity.ConsultationClinicalNote;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.repository.ConsultationClinicalNoteRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock ConsultationAccessService accessService;
    @Mock ConsultationResponseMapper responseMapper;
    @Mock ConsultationClinicalNoteRepository noteRepository;
    @Mock ConsultationRealtimePublisher realtimePublisher;
    @Mock PrescriptionRepository prescriptionRepository;
    @Mock VideoConsultationService videoConsultationService;

    private ConsultationService service;
    private Jwt jwt;
    private ConsultationAccessService.ConsultationContext context;

    @BeforeEach
    void setUp() {
        service = new ConsultationService(accessService, responseMapper, noteRepository, realtimePublisher,
                prescriptionRepository, videoConsultationService);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        context = scheduledContext();
    }

    @Test
    void assignedDoctorStartsAndCompletesConsultation() {
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);

        service.start(jwt, context.consultation().getId());
        assertThat(context.consultation().getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);

        service.complete(jwt, context.consultation().getId());
        assertThat(context.consultation().getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
        verify(realtimePublisher, org.mockito.Mockito.times(2))
                .publishAfterCommit(any(Appointment.class), any());
    }

    @Test
    void scheduledConsultationCannotBeCompletedDirectly() {
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);

        assertThatThrownBy(() -> service.complete(jwt, context.consultation().getId()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void draftBlocksCompletionUntilResolved() {
        context.consultation().start();
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(prescriptionRepository.existsByConsultationIdAndStatus(
                context.consultation().getId(), com.medisync.prescription.entity.PrescriptionStatus.DRAFT))
                .thenReturn(true, false);

        assertThatThrownBy(() -> service.complete(jwt, context.consultation().getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Discard or issue");
        assertThat(context.consultation().getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);

        service.complete(jwt, context.consultation().getId());
        assertThat(context.consultation().getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
    }

    @Test
    void doctorCreatesAndUpdatesPrivateNoteBeforeCompletion() {
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(noteRepository.findByConsultationIdForUpdate(context.consultation().getId()))
                .thenReturn(Optional.empty());
        when(noteRepository.save(any(ConsultationClinicalNote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateClinicalNote(jwt, context.consultation().getId(),
                new UpdateClinicalNoteRequest("  Patient reports improvement.  "));

        assertThat(response.noteText()).isEqualTo("Patient reports improvement.");
        assertThat(response.finalized()).isFalse();
    }

    @Test
    void completedClinicalNoteIsReadOnly() {
        context.consultation().start();
        context.consultation().complete();
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);

        assertThatThrownBy(() -> service.updateClinicalNote(jwt, context.consultation().getId(),
                new UpdateClinicalNoteRequest("Rewrite")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("read-only");
    }

    private ConsultationAccessService.ConsultationContext scheduledContext() {
        AppUser patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        AppUser doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        PatientProfile patient = new PatientProfile(patientUser.getId());
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());
        doctor.verify(UUID.randomUUID());
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(), start,
                start.plusMinutes(30));
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        return new ConsultationAccessService.ConsultationContext(
                consultation, appointment, patient, doctor, patientUser, doctorUser);
    }
}
