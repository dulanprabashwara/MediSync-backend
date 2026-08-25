package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.consultation.dto.ConsultationSenderType;
import com.medisync.consultation.dto.SendMessageRequest;
import com.medisync.consultation.entity.ConsultationMessage;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.repository.ConsultationMessageRepository;
import com.medisync.consultation.repository.ConsultationMessageAttachmentRepository;
import com.medisync.audit.service.AuditService;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.media.ImageUploadValidator;
import com.medisync.media.MediaStorageService;
import com.medisync.media.MediaUrlService;
import com.medisync.media.ValidatedImage;
import com.medisync.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class ConsultationChatServiceTest {

    @Mock ConsultationAccessService accessService;
    @Mock ConsultationMessageRepository messageRepository;
    @Mock ConsultationRealtimePublisher realtimePublisher;
    @Mock ConsultationMessageAttachmentRepository attachmentRepository;
    @Mock ImageUploadValidator imageValidator;
    @Mock MediaStorageService storageService;
    @Mock MediaUrlService mediaUrlService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    private ConsultationChatService service;
    private Jwt jwt;
    private ConsultationAccessService.ConsultationContext context;

    @BeforeEach
    void setUp() {
        service = new ConsultationChatService(accessService, messageRepository, realtimePublisher,
                attachmentRepository, imageValidator, storageService, mediaUrlService, auditService, notificationService);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        context = scheduledContext();
    }

    @Test
    void patientMessageIsTrimmedPersistedAndPublished() {
        when(accessService.requirePatient(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(messageRepository.save(any(ConsultationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.sendPatientMessage(jwt, context.consultation().getId(),
                new SendMessageRequest("  Hello doctor  "));

        assertThat(response.content()).isEqualTo("Hello doctor");
        assertThat(response.senderType()).isEqualTo(ConsultationSenderType.PATIENT);
        InOrder order = org.mockito.Mockito.inOrder(messageRepository, realtimePublisher);
        order.verify(messageRepository).save(any(ConsultationMessage.class));
        order.verify(realtimePublisher).publishAfterCommit(any(Appointment.class), any());
        verify(accessService).requireChatWritable(context);
    }

    @Test
    void blankAndOverlongMessagesAreRejectedByServiceDefense() {
        when(accessService.requirePatient(jwt, context.consultation().getId(), true)).thenReturn(context);

        assertThatThrownBy(() -> service.sendPatientMessage(jwt, context.consultation().getId(),
                new SendMessageRequest("   "))).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.sendPatientMessage(jwt, context.consultation().getId(),
                new SendMessageRequest("x".repeat(4001)))).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void completedConsultationStillAllowsMessages() {
        context.consultation().start();
        context.consultation().complete();
        when(accessService.requirePatient(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(messageRepository.save(any(ConsultationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var patientResponse = service.sendPatientMessage(jwt, context.consultation().getId(),
                new SendMessageRequest("Thank you doctor"));
        var doctorResponse = service.sendDoctorMessage(jwt, context.consultation().getId(),
                new SendMessageRequest("Continue hydration"));

        assertThat(patientResponse.senderType()).isEqualTo(ConsultationSenderType.PATIENT);
        assertThat(doctorResponse.senderType()).isEqualTo(ConsultationSenderType.DOCTOR);
        verify(accessService, org.mockito.Mockito.times(2)).requireChatWritable(context);
    }

    @Test
    void cancelledConsultationIsReadOnly() {
        context.consultation().cancel();
        when(accessService.requirePatient(jwt, context.consultation().getId(), true)).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResourceConflictException("read-only"))
                .when(accessService).requireChatWritable(context);

        assertThatThrownBy(() -> service.sendPatientMessage(jwt, context.consultation().getId(),
                new SendMessageRequest("Hello"))).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void completedConsultationAllowsPrivateImageOnlyMessage() {
        context.consultation().start();
        context.consultation().complete();
        when(accessService.requirePatient(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(messageRepository.save(any(ConsultationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        byte[] bytes = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};
        MockMultipartFile upload = new MockMultipartFile("images", "receipt.jpg", "image/jpeg", bytes);
        when(imageValidator.validate(upload)).thenReturn(
                new ValidatedImage(bytes, "image/jpeg", "jpg", "receipt.jpg"));
        when(mediaUrlService.signedUrlOrNull(any())).thenAnswer(invocation ->
                invocation.getArgument(0) == null ? null : "https://signed.example/private");

        var response = service.sendPatientMedia(jwt, context.consultation().getId(), "", java.util.List.of(upload));

        assertThat(response.content()).isNull();
        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).signedUrl()).startsWith("https://signed.example/");
        verify(storageService).upload(anyString(), any(ValidatedImage.class));
        verify(attachmentRepository).saveAllAndFlush(any());
        verify(auditService).record(org.mockito.Mockito.eq(context.patientUser()),
                org.mockito.Mockito.eq(com.medisync.audit.AuditActions.CHAT_IMAGES_SENT),
                org.mockito.Mockito.eq("CONSULTATION"), org.mockito.Mockito.eq(context.consultation().getId()), any());
    }

    @Test
    void patientSendingMediaTriggersAttachmentNotification() {
        context.consultation().start();
        when(accessService.requirePatient(jwt, context.consultation().getId(), true)).thenReturn(context);
        when(messageRepository.save(any(ConsultationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        byte[] bytes = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};
        MockMultipartFile upload = new MockMultipartFile("images", "scan.jpg", "image/jpeg", bytes);
        when(imageValidator.validate(upload)).thenReturn(
                new ValidatedImage(bytes, "image/jpeg", "jpg", "scan.jpg"));
        when(mediaUrlService.signedUrlOrNull(any())).thenAnswer(invocation ->
                invocation.getArgument(0) == null ? null : "https://signed.example/private");

        service.sendPatientMedia(jwt, context.consultation().getId(), "Here is my scan", java.util.List.of(upload));

        verify(notificationService).createNotification(
                org.mockito.Mockito.eq(context.doctorUser().getId()),
                org.mockito.Mockito.eq(context.patientUser().getId()),
                org.mockito.Mockito.eq(com.medisync.notification.NotificationType.NEW_CHAT_ATTACHMENT),
                org.mockito.Mockito.eq("New chat attachment"),
                anyString(),
                anyString(),
                org.mockito.Mockito.eq("CONSULTATION"),
                org.mockito.Mockito.eq(context.consultation().getId()),
                anyString()
        );
    }

    @Test
    void cancelledConsultationBlocksImageBeforeStorage() {
        context.consultation().cancel();
        when(accessService.requireDoctor(jwt, context.consultation().getId(), true)).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResourceConflictException("read-only"))
                .when(accessService).requireChatWritable(context);
        MockMultipartFile upload = new MockMultipartFile("images", "image.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});

        assertThatThrownBy(() -> service.sendDoctorMedia(jwt, context.consultation().getId(), null,
                java.util.List.of(upload))).isInstanceOf(ResourceConflictException.class);
        verify(storageService, org.mockito.Mockito.never()).upload(anyString(), any());
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
