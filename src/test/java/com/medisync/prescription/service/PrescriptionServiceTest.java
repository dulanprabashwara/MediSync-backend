package com.medisync.prescription.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.service.ConsultationAccessService;
import com.medisync.audit.service.AuditService;
import com.medisync.config.PrescriptionPaymentProperties;
import com.medisync.exception.ResourceConflictException;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.dto.PrescriptionDraftRequest;
import com.medisync.prescription.dto.PrescriptionItemRequest;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionQrToken;
import com.medisync.prescription.entity.DoctorFeeStatus;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionItemRepository;
import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock PrescriptionRepository prescriptionRepository;
    @Mock PrescriptionItemRepository itemRepository;
    @Mock PrescriptionQrTokenRepository tokenRepository;
    @Mock PrescriptionDispensationRepository dispensationRepository;
    @Mock PrescriptionAccessService accessService;
    @Mock ConsultationAccessService consultationAccessService;
    @Mock PrescriptionResponseMapper mapper;
    @Mock PrescriptionQrTokenGenerator tokenGenerator;
    @Mock CurrentUserService currentUserService;
    @Mock AuditService auditService;
    @Mock com.medisync.consultation.service.ConsultationRealtimePublisher realtimePublisher;

    private final PrescriptionQrTokenHasher tokenHasher = new PrescriptionQrTokenHasher();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
    private PrescriptionService service;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptionRepository, itemRepository, tokenRepository,
                dispensationRepository, accessService, consultationAccessService, mapper, tokenGenerator,
                tokenHasher, Clock.fixed(now.toInstant(), ZoneId.of("UTC")),
                new PrescriptionPaymentProperties("LKR"), currentUserService, auditService, realtimePublisher);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(now.toInstant()).expiresAt(now.plusMinutes(5).toInstant()).build();
    }

    @Test
    void scheduledAndInProgressConsultationsCanCreateDrafts() {
        for (ConsultationStatus status : List.of(ConsultationStatus.SCHEDULED, ConsultationStatus.IN_PROGRESS)) {
            var context = context(status);
            when(consultationAccessService.requireDoctor(jwt, context.consultation().getId(), true))
                    .thenReturn(context);
            when(prescriptionRepository.findByConsultationIdAndStatus(
                    context.consultation().getId(), PrescriptionStatus.DRAFT)).thenReturn(Optional.empty());
            when(prescriptionRepository.saveAndFlush(any(Prescription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertThatCode(() -> service.createDraft(jwt, context.consultation().getId()))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void completedAndCancelledConsultationsRejectDraftCreation() {
        for (ConsultationStatus status : List.of(ConsultationStatus.COMPLETED, ConsultationStatus.CANCELLED)) {
            var context = context(status);
            when(consultationAccessService.requireDoctor(jwt, context.consultation().getId(), true))
                    .thenReturn(context);

            assertThatThrownBy(() -> service.createDraft(jwt, context.consultation().getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Test
    void draftEditingIsAllowedOnlyScheduledOrInProgress() {
        var scheduled = context(ConsultationStatus.SCHEDULED);
        Prescription writable = prescription(scheduled);
        allowDoctorPrescription(writable, scheduled);
        when(prescriptionRepository.saveAndFlush(writable)).thenReturn(writable);

        service.updateDraft(jwt, writable.getId(), request());
        verify(itemRepository).saveAllAndFlush(any());

        var completed = context(ConsultationStatus.COMPLETED);
        Prescription locked = prescription(completed);
        allowDoctorPrescription(locked, completed);
        assertThatThrownBy(() -> service.updateDraft(jwt, locked.getId(), request()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void issueIsAllowedOnlyWhileConsultationIsInProgressAndDoesNotCreateQr() {
        var inProgress = context(ConsultationStatus.IN_PROGRESS);
        Prescription prescription = prescription(inProgress);
        allowDoctorPrescription(prescription, inProgress);
        when(itemRepository.countByPrescriptionId(prescription.getId())).thenReturn(1L);

        service.issue(jwt, prescription.getId());

        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        verify(tokenRepository, never()).saveAndFlush(any());

        for (ConsultationStatus status : List.of(ConsultationStatus.SCHEDULED, ConsultationStatus.COMPLETED,
                ConsultationStatus.CANCELLED)) {
            var context = context(status);
            Prescription draft = prescription(context);
            allowDoctorPrescription(draft, context);
            assertThatThrownBy(() -> service.issue(jwt, draft.getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Test
    void assignedDoctorCanDiscardOnlyDraftIncludingCompletedLegacyDraft() {
        var completed = context(ConsultationStatus.COMPLETED);
        Prescription draft = prescription(completed);
        allowDoctorPrescription(draft, completed);

        service.discardDraft(jwt, draft.getId());

        verify(itemRepository).deleteByPrescriptionId(draft.getId());
        verify(prescriptionRepository).delete(draft);

        var inProgress = context(ConsultationStatus.IN_PROGRESS);
        Prescription issued = prescription(inProgress);
        issued.issue(now);
        allowDoctorPrescription(issued, inProgress);
        assertThatThrownBy(() -> service.discardDraft(jwt, issued.getId()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void qrGenerationStoresOnlyHashAndRotationReplacesIt() {
        var context = context(ConsultationStatus.IN_PROGRESS);
        Prescription prescription = prescription(context);
        prescription.issue(now.minusDays(1));
        when(accessService.requirePatient(jwt)).thenReturn(context.patient());
        when(accessService.requirePatientPrescription(context.patient(), prescription.getId(), true))
                .thenReturn(new PrescriptionAccessService.PatientPrescriptionAccess(
                        prescription, context.patient()));
        when(tokenGenerator.generateToken()).thenReturn("raw-token-A", "raw-token-B");
        when(tokenGenerator.payload(any())).thenAnswer(invocation -> "MEDISYNC:RX:" + invocation.getArgument(0));
        when(tokenRepository.existsByTokenHash(any())).thenReturn(false);
        AtomicReference<PrescriptionQrToken> stored = new AtomicReference<>();
        when(tokenRepository.findByPrescriptionIdForUpdate(prescription.getId())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get()));
        when(tokenRepository.saveAndFlush(any(PrescriptionQrToken.class))).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return stored.get();
        });

        var first = service.generatePatientQr(jwt, prescription.getId());
        String firstHash = stored.get().getTokenHash();
        var second = service.generatePatientQr(jwt, prescription.getId());

        assertThat(first.qrPayload()).isEqualTo("MEDISYNC:RX:raw-token-A");
        assertThat(firstHash).isEqualTo(tokenHasher.hash("raw-token-A"));
        assertThat(stored.get().getTokenHash()).isEqualTo(tokenHasher.hash("raw-token-B"))
                .isNotEqualTo(firstHash);
        assertThat(second.qrPayload()).isEqualTo("MEDISYNC:RX:raw-token-B");
        assertThat(stored.get().getTokenHash()).doesNotContain("raw-token");
    }

    @Test
    void positiveFeeBlocksQrUntilAssignedDoctorConfirmsAfterCompletion() {
        var context = context(ConsultationStatus.COMPLETED);
        Prescription prescription = prescription(context);
        prescription.updateDraft(30, null, new BigDecimal("1500.00"), "LKR");
        prescription.issue(now.minusDays(1));
        when(accessService.requirePatient(jwt)).thenReturn(context.patient());
        when(accessService.requirePatientPrescription(context.patient(), prescription.getId(), true))
                .thenReturn(new PrescriptionAccessService.PatientPrescriptionAccess(prescription, context.patient()));

        assertThatThrownBy(() -> service.generatePatientQr(jwt, prescription.getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("confirm");

        allowDoctorPrescription(prescription, context);
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(context.doctorUser());
        service.confirmDoctorFee(jwt, prescription.getId());

        assertThat(prescription.getDoctorFeeStatus()).isEqualTo(DoctorFeeStatus.CONFIRMED);
        assertThat(prescription.isQrPaymentEligible()).isTrue();
        verify(auditService).record(org.mockito.Mockito.eq(context.doctorUser()),
                org.mockito.Mockito.eq(com.medisync.audit.AuditActions.DOCTOR_FEE_CONFIRMED),
                org.mockito.Mockito.eq("PRESCRIPTION"), org.mockito.Mockito.eq(prescription.getId()), any());
    }

    @Test
    void expiredOrDispensedPrescriptionCannotHavePaymentConfirmed() {
        var context = context(ConsultationStatus.COMPLETED);
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(context.doctorUser());

        Prescription expired = prescription(context);
        expired.updateDraft(30, null, new BigDecimal("500.00"), "LKR");
        expired.issue(now.minusDays(31));
        allowDoctorPrescription(expired, context);
        assertThatThrownBy(() -> service.confirmDoctorFee(jwt, expired.getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("expired");

        Prescription dispensed = prescription(context);
        dispensed.updateDraft(30, null, new BigDecimal("500.00"), "LKR");
        dispensed.issue(now.minusDays(1));
        allowDoctorPrescription(dispensed, context);
        when(dispensationRepository.existsByPrescriptionId(dispensed.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.confirmDoctorFee(jwt, dispensed.getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("dispensed");
    }

    @Test
    void cancelledAndExpiredPrescriptionsCannotGenerateQr() {
        var context = context(ConsultationStatus.IN_PROGRESS);
        Prescription cancelled = prescription(context);
        cancelled.issue(now.minusDays(1));
        cancelled.cancel("Changed treatment", now);
        when(accessService.requirePatient(jwt)).thenReturn(context.patient());
        when(accessService.requirePatientPrescription(context.patient(), cancelled.getId(), true))
                .thenReturn(new PrescriptionAccessService.PatientPrescriptionAccess(cancelled, context.patient()));
        assertThatThrownBy(() -> service.generatePatientQr(jwt, cancelled.getId()))
                .isInstanceOf(ResourceConflictException.class);

        Prescription expired = prescription(context);
        expired.issue(now.minusDays(31));
        when(accessService.requirePatientPrescription(context.patient(), expired.getId(), true))
                .thenReturn(new PrescriptionAccessService.PatientPrescriptionAccess(expired, context.patient()));
        assertThatThrownBy(() -> service.generatePatientQr(jwt, expired.getId()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void dispensedPrescriptionCannotGenerateQrOrBeCancelled() {
        var context = context(ConsultationStatus.IN_PROGRESS);
        Prescription prescription = prescription(context);
        prescription.issue(now.minusDays(1));
        when(dispensationRepository.existsByPrescriptionId(prescription.getId())).thenReturn(true);
        when(accessService.requirePatient(jwt)).thenReturn(context.patient());
        when(accessService.requirePatientPrescription(context.patient(), prescription.getId(), true))
                .thenReturn(new PrescriptionAccessService.PatientPrescriptionAccess(prescription, context.patient()));

        assertThatThrownBy(() -> service.generatePatientQr(jwt, prescription.getId()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already been dispensed");

        when(accessService.requireDoctor(jwt)).thenReturn(context.doctor());
        when(accessService.requireDoctorPrescription(context.doctor(), prescription.getId(), true))
                .thenReturn(new PrescriptionAccessService.DoctorPrescriptionAccess(prescription, context.doctor()));
        when(consultationAccessService.requireDoctor(jwt, prescription.getConsultationId(), true))
                .thenReturn(context);
        assertThatThrownBy(() -> service.cancel(jwt, prescription.getId(), "Treatment changed"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("dispensed prescription");
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    private void allowDoctorPrescription(Prescription prescription,
                                         ConsultationAccessService.ConsultationContext context) {
        when(accessService.requireDoctorPrescription(jwt, prescription.getId(), true))
                .thenReturn(new PrescriptionAccessService.DoctorPrescriptionAccess(
                        prescription, context.doctor()));
        when(consultationAccessService.requireDoctor(jwt, prescription.getConsultationId(), true))
                .thenReturn(context);
    }

    private PrescriptionDraftRequest request() {
        return new PrescriptionDraftRequest(30, "After meals", List.of(new PrescriptionItemRequest(
                "Medicine", "500 mg", "Tablet", "One tablet", "Twice daily", "5 days",
                "10", "Oral", "With water")));
    }

    private Prescription prescription(ConsultationAccessService.ConsultationContext context) {
        return new Prescription(context.consultation().getId(), context.doctor().getId(), context.patient().getId());
    }

    private ConsultationAccessService.ConsultationContext context(ConsultationStatus status) {
        AppUser patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        AppUser doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        PatientProfile patient = new PatientProfile(patientUser.getId());
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());
        doctor.verify(UUID.randomUUID());
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(),
                now.plusHours(1), now.plusHours(2));
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        if (status == ConsultationStatus.IN_PROGRESS || status == ConsultationStatus.COMPLETED) {
            consultation.start();
        }
        if (status == ConsultationStatus.COMPLETED) consultation.complete();
        if (status == ConsultationStatus.CANCELLED) consultation.cancel();
        return new ConsultationAccessService.ConsultationContext(
                consultation, appointment, patient, doctor, patientUser, doctorUser);
    }
}
