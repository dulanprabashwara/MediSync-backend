package com.medisync.consultation.service;

import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.consultation.dto.ConsultationMessageResponse;
import com.medisync.consultation.dto.ConsultationSenderType;
import com.medisync.consultation.dto.SendMessageRequest;
import com.medisync.consultation.entity.ConsultationMessage;
import com.medisync.consultation.repository.ConsultationMessageRepository;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConsultationChatService {

    public static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConsultationAccessService accessService;
    private final ConsultationMessageRepository messageRepository;
    private final ConsultationRealtimePublisher realtimePublisher;

    public ConsultationChatService(ConsultationAccessService accessService,
                                   ConsultationMessageRepository messageRepository,
                                   ConsultationRealtimePublisher realtimePublisher) {
        this.accessService = accessService;
        this.messageRepository = messageRepository;
        this.realtimePublisher = realtimePublisher;
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
        return send(context, context.patientUser().getId(), request.content());
    }

    @Transactional
    public ConsultationMessageResponse sendDoctorMessage(Jwt jwt, UUID consultationId,
                                                         SendMessageRequest request) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, true);
        return send(context, context.doctorUser().getId(), request.content());
    }

    private PageResponse<ConsultationMessageResponse> messages(
            ConsultationAccessService.ConsultationContext context, int page, int size) {
        validatePage(page, size);
        Page<ConsultationMessage> messages = messageRepository
                .findByConsultationIdOrderBySentAtDescIdDesc(
                        context.consultation().getId(), PageRequest.of(page, size));
        List<ConsultationMessageResponse> chronological = new ArrayList<>(messages.getContent().stream()
                .map(message -> toResponse(context, message))
                .toList());
        Collections.reverse(chronological);
        return new PageResponse<>(chronological, messages.getNumber(), messages.getSize(), messages.getTotalElements(),
                messages.getTotalPages(), messages.isFirst(), messages.isLast());
    }

    private ConsultationMessageResponse send(ConsultationAccessService.ConsultationContext context,
                                             UUID senderUserId, String rawContent) {
        accessService.requireChatWritable(context);
        String content = validateContent(rawContent);
        ConsultationMessage message = messageRepository.save(
                new ConsultationMessage(context.consultation().getId(), senderUserId, content));
        ConsultationMessageResponse response = toResponse(context, message);
        realtimePublisher.publishAfterCommit(context.appointment(), ConsultationEvent.newMessage(response));
        return response;
    }

    private ConsultationMessageResponse toResponse(ConsultationAccessService.ConsultationContext context,
                                                   ConsultationMessage message) {
        ConsultationSenderType senderType;
        String senderDisplayName;
        if (message.getSenderUserId().equals(context.patientUser().getId())) {
            senderType = ConsultationSenderType.PATIENT;
            senderDisplayName = fullName(context.patientUser());
        } else if (message.getSenderUserId().equals(context.doctorUser().getId())) {
            senderType = ConsultationSenderType.DOCTOR;
            senderDisplayName = "Dr. " + fullName(context.doctorUser());
        } else {
            throw new ResourceConflictException("Message sender is not a consultation participant");
        }
        return new ConsultationMessageResponse(message.getId(), message.getConsultationId(), senderType,
                senderDisplayName, message.getContent(), message.getSentAt());
    }

    private String validateContent(String value) {
        String content = value == null ? "" : value.trim();
        if (content.isEmpty()) {
            throw new InvalidRequestException("Message content is required");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidRequestException("Message content must not exceed 4000 characters");
        }
        return content;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100");
        }
    }

    private String fullName(com.medisync.user.entity.AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
