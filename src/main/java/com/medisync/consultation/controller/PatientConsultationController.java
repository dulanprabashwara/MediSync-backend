package com.medisync.consultation.controller;

import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.dto.ConsultationMessageResponse;
import com.medisync.consultation.dto.ConsultationResponse;
import com.medisync.consultation.dto.SendMessageRequest;
import com.medisync.consultation.service.ConsultationChatService;
import com.medisync.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/patient/consultations")
public class PatientConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationChatService chatService;

    public PatientConsultationController(ConsultationService consultationService,
                                         ConsultationChatService chatService) {
        this.consultationService = consultationService;
        this.chatService = chatService;
    }

    @GetMapping("/{consultationId}")
    public ConsultationResponse details(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID consultationId) {
        return consultationService.patientDetails(jwt, consultationId);
    }

    @GetMapping("/{consultationId}/messages")
    public PageResponse<ConsultationMessageResponse> messages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return chatService.patientMessages(jwt, consultationId, page, size);
    }

    @PostMapping(value = "/{consultationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationMessageResponse sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendPatientMessage(jwt, consultationId, request);
    }

    @PostMapping(value = "/{consultationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationMessageResponse sendMediaMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return chatService.sendPatientMedia(jwt, consultationId, content, images);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{consultationId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @PathVariable UUID messageId) {
        chatService.deletePatientMessage(jwt, consultationId, messageId);
    }
}
