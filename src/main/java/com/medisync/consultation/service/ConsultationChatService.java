package com.medisync.consultation.service;

import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.consultation.dto.ConsultationMessageResponse;
import com.medisync.consultation.dto.ConsultationMessageAttachmentResponse;
import com.medisync.consultation.dto.ConsultationSenderType;
import com.medisync.consultation.dto.SendMessageRequest;
import com.medisync.consultation.entity.ConsultationMessage;
import com.medisync.consultation.entity.ConsultationMessageAttachment;
import com.medisync.consultation.repository.ConsultationMessageAttachmentRepository;
import com.medisync.consultation.repository.ConsultationMessageRepository;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medisync.notification.service.NotificationService;
import com.medisync.notification.NotificationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import com.medisync.media.ImageUploadValidator;
import com.medisync.media.MediaStorageService;
import com.medisync.media.MediaStorageUnavailableException;
import com.medisync.media.MediaUrlService;
import com.medisync.media.ValidatedImage;
import com.medisync.audit.AuditActions;
import com.medisync.audit.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ConsultationChatService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationChatService.class);

    public static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConsultationAccessService accessService;
    private final ConsultationMessageRepository messageRepository;
    private final ConsultationRealtimePublisher realtimePublisher;
    private final ConsultationMessageAttachmentRepository attachmentRepository;
    private final ImageUploadValidator imageValidator;
    private final MediaStorageService storageService;
    private final MediaUrlService mediaUrlService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Autowired
    public ConsultationChatService(ConsultationAccessService accessService,
                                   ConsultationMessageRepository messageRepository,
                                   ConsultationRealtimePublisher realtimePublisher,
                                   ConsultationMessageAttachmentRepository attachmentRepository,
                                   ImageUploadValidator imageValidator,
                                   MediaStorageService storageService,
                                   MediaUrlService mediaUrlService,
                                   AuditService auditService,
                                   NotificationService notificationService) {
        this.accessService = accessService;
        this.messageRepository = messageRepository;
        this.realtimePublisher = realtimePublisher;
        this.attachmentRepository = attachmentRepository;
        this.imageValidator = imageValidator;
        this.storageService = storageService;
        this.mediaUrlService = mediaUrlService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    ConsultationChatService(ConsultationAccessService accessService,
                            ConsultationMessageRepository messageRepository,
                            ConsultationRealtimePublisher realtimePublisher) {
        this(accessService, messageRepository, realtimePublisher, null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultationMessageResponse> patientMessages(Jwt jwt, UUID consultationId,
                                                                     int page, int size) {
        return messages(accessService.requirePatient(jwt, consultationId, false), page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultationMessageResponse> doctorMessages(Jwt jwt, UUID consultationId,
                                                                    int page, int size) {
        return messages(accessService.requireDoctor(jwt, consultationId, false), page, size);
    }

    @Transactional
    public ConsultationMessageResponse sendPatientMessage(Jwt jwt, UUID consultationId,
                                                          SendMessageRequest request) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requirePatient(jwt, consultationId, true);
        return send(context, context.patientUser().getId(), request.content(), List.of());
    }

    @Transactional
    public ConsultationMessageResponse sendDoctorMessage(Jwt jwt, UUID consultationId,
                                                         SendMessageRequest request) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, true);
        return send(context, context.doctorUser().getId(), request.content(), List.of());
    }

    @Transactional
    public ConsultationMessageResponse sendPatientMedia(Jwt jwt, UUID consultationId,
                                                        String content, List<MultipartFile> images) {
        var context = accessService.requirePatient(jwt, consultationId, true);
        return send(context, context.patientUser().getId(), content, safeFiles(images));
    }

    @Transactional
    public ConsultationMessageResponse sendDoctorMedia(Jwt jwt, UUID consultationId,
                                                       String content, List<MultipartFile> images) {
        var context = accessService.requireDoctor(jwt, consultationId, true);
        return send(context, context.doctorUser().getId(), content, safeFiles(images));
    }

    private PageResponse<ConsultationMessageResponse> messages(
            ConsultationAccessService.ConsultationContext context, int page, int size) {
        validatePage(page, size);
        Page<ConsultationMessage> messages = messageRepository
                .findByConsultationIdOrderBySentAtDescIdDesc(
                        context.consultation().getId(), PageRequest.of(page, size));
        Map<UUID, List<ConsultationMessageAttachment>> attachmentMap = attachmentRepository == null
                ? Map.of()
                : attachmentRepository.findByMessageIdInOrderByMessageIdAscPositionAsc(
                                messages.getContent().stream().map(ConsultationMessage::getId).toList())
                        .stream().collect(Collectors.groupingBy(ConsultationMessageAttachment::getMessageId));
        List<ConsultationMessageResponse> chronological = new ArrayList<>(messages.getContent().stream()
                .map(message -> toResponse(context, message, attachmentMap.getOrDefault(message.getId(), List.of())))
                .toList());
        Collections.reverse(chronological);
        return new PageResponse<>(chronological, messages.getNumber(), messages.getSize(), messages.getTotalElements(),
                messages.getTotalPages(), messages.isFirst(), messages.isLast());
    }

    private ConsultationMessageResponse send(ConsultationAccessService.ConsultationContext context,
                                             UUID senderUserId, String rawContent, List<MultipartFile> files) {
        accessService.requireChatWritable(context);
        if (files.size() > 4) {
            throw new InvalidRequestException("A message can contain at most 4 images");
        }
        String content = validateContent(rawContent, !files.isEmpty());
        List<ValidatedImage> images = files.isEmpty()
                ? List.of()
                : files.stream().map(imageValidator::validate).toList();
        ConsultationMessage message = messageRepository.save(
                new ConsultationMessage(context.consultation().getId(), senderUserId, content));
        List<String> uploadedKeys = new ArrayList<>();
        List<ConsultationMessageAttachment> attachments = new ArrayList<>();
        try {
            for (int index = 0; index < images.size(); index++) {
                ValidatedImage image = images.get(index);
                String key = "consultations/" + context.consultation().getId() + "/messages/"
                        + message.getId() + "/" + UUID.randomUUID() + "." + image.extension();
                storageService.upload(key, image);
                uploadedKeys.add(key);
                attachments.add(new ConsultationMessageAttachment(message.getId(), key, image.contentType(),
                        image.originalFilename(), image.bytes().length, index + 1));
            }
            if (!attachments.isEmpty()) {
                attachmentRepository.saveAllAndFlush(attachments);
            }
        } catch (RuntimeException exception) {
            uploadedKeys.forEach(this::bestEffortDelete);
            throw exception;
        }
        ConsultationMessageResponse response = toResponse(context, message, attachments);
        var actor = senderUserId.equals(context.patientUser().getId())
                ? context.patientUser() : context.doctorUser();
        if (auditService != null) {
            auditService.record(actor,
                    attachments.isEmpty() ? AuditActions.CHAT_MESSAGE_SENT : AuditActions.CHAT_IMAGES_SENT,
                    "CONSULTATION", context.consultation().getId(),
                    Map.of("attachmentCount", attachments.size()));
        }

        if (!attachments.isEmpty() && senderUserId.equals(context.patientUser().getId()) && notificationService != null) {
            // Notification: NEW_CHAT_ATTACHMENT -> Doctor
            String patientName = context.patientUser().getFirstName() + " " + context.patientUser().getLastName();
            notificationService.createNotification(
                    context.doctorUser().getId(),
                    context.patientUser().getId(),
                    NotificationType.NEW_CHAT_ATTACHMENT,
                    "New chat attachment",
                    patientName + " sent a new attachment.",
                    "/doctor/consultations/" + context.consultation().getId(),
                    "CONSULTATION",
                    context.consultation().getId(),
                    "consultation:" + context.consultation().getId() + ":attachment:" + message.getId()
            );
        }

        realtimePublisher.publishAfterCommit(context.appointment(), ConsultationEvent.newMessage(response));
        return response;
    }

    private ConsultationMessageResponse toResponse(ConsultationAccessService.ConsultationContext context,
                                                   ConsultationMessage message,
                                                   List<ConsultationMessageAttachment> attachments) {
        ConsultationSenderType senderType;
        String senderDisplayName;
        String senderProfileImageUrl;
        if (message.getSenderUserId().equals(context.patientUser().getId())) {
            senderType = ConsultationSenderType.PATIENT;
            senderDisplayName = fullName(context.patientUser());
            senderProfileImageUrl = mediaUrlService == null ? null
                    : mediaUrlService.signedUrlOrNull(context.patientUser().getProfileImageKey());
        } else if (message.getSenderUserId().equals(context.doctorUser().getId())) {
            senderType = ConsultationSenderType.DOCTOR;
            senderDisplayName = "Dr. " + fullName(context.doctorUser());
            senderProfileImageUrl = mediaUrlService == null ? null
                    : mediaUrlService.signedUrlOrNull(context.doctorUser().getProfileImageKey());
        } else {
            throw new ResourceConflictException("Message sender is not a consultation participant");
        }
        if (message.isDeleted()) {
            return new ConsultationMessageResponse(message.getId(), message.getConsultationId(), senderType,
                    senderDisplayName, senderProfileImageUrl, null, List.of(), message.getSentAt(), true, message.getDeletedAt());
        }

        List<ConsultationMessageAttachmentResponse> attachmentResponses = attachments.stream()
                .map(attachment -> new ConsultationMessageAttachmentResponse(attachment.getId(),
                        attachment.getContentType(), attachment.getOriginalFilename(), attachment.getByteSize(),
                        attachment.getPosition(), mediaUrlService == null
                                ? null : mediaUrlService.signedUrlOrNull(attachment.getStorageKey())))
                .toList();
        return new ConsultationMessageResponse(message.getId(), message.getConsultationId(), senderType,
                senderDisplayName, senderProfileImageUrl, message.getContent(), attachmentResponses, message.getSentAt(), false, null);
    }

    private String validateContent(String value, boolean hasImages) {
        String content = value == null ? "" : value.trim();
        if (content.isEmpty() && !hasImages) {
            throw new InvalidRequestException("Message content is required");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidRequestException("Message content must not exceed 4000 characters");
        }
        return content.isEmpty() ? null : content;
    }

    private List<MultipartFile> safeFiles(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
    }

    private void bestEffortDelete(String key) {
        try {
            storageService.delete(key);
        } catch (MediaStorageUnavailableException exception) {
            log.warn("An orphaned chat image could not be removed");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100");
        }
    }

    @Transactional
    public void deletePatientMessage(Jwt jwt, UUID consultationId, UUID messageId) {
        var context = accessService.requirePatient(jwt, consultationId, true);
        deleteMessage(context, messageId, context.patientUser().getId());
    }

    @Transactional
    public void deleteDoctorMessage(Jwt jwt, UUID consultationId, UUID messageId) {
        var context = accessService.requireDoctor(jwt, consultationId, true);
        deleteMessage(context, messageId, context.doctorUser().getId());
    }

    private void deleteMessage(ConsultationAccessService.ConsultationContext context, UUID messageId, UUID senderUserId) {
        accessService.requireChatWritable(context);
        ConsultationMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidRequestException("Message not found"));

        if (!message.getConsultationId().equals(context.consultation().getId())) {
            throw new InvalidRequestException("Message does not belong to this consultation");
        }
        if (!message.getSenderUserId().equals(senderUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only delete your own messages");
        }
        if (message.isDeleted()) {
            return; // Idempotent
        }

        message.softDelete();
        messageRepository.saveAndFlush(message);

        // We choose to leave attachments in the DB and Storage to preserve history, 
        // but we won't serve them in the API anymore because of the isDeleted() check.
        // We only notify the frontend.
        ConsultationMessageResponse deletedResponse = toResponse(context, message, List.of());
        realtimePublisher.publishAfterCommit(context.appointment(), ConsultationEvent.messageDeleted(deletedResponse));
    }

    private String fullName(com.medisync.user.entity.AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
