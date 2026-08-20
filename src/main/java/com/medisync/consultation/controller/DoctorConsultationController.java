package com.medisync.consultation.controller;

import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.dto.ClinicalNoteResponse;
import com.medisync.consultation.dto.ConsultationMessageResponse;
import com.medisync.consultation.dto.ConsultationResponse;
import com.medisync.consultation.dto.SendMessageRequest;
import com.medisync.consultation.dto.UpdateClinicalNoteRequest;
import com.medisync.consultation.service.ConsultationChatService;
import com.medisync.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/consultations")
public class DoctorConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationChatService chatService;

    public DoctorConsultationController(ConsultationService consultationService,
                                        ConsultationChatService chatService) {
        this.consultationService = consultationService;
        this.chatService = chatService;
    }

    @GetMapping("/{consultationId}")
    public ConsultationResponse details(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID consultationId) {
        return consultationService.doctorDetails(jwt, consultationId);
    }

    @GetMapping("/{consultationId}/messages")
    public PageResponse<ConsultationMessageResponse> messages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return chatService.doctorMessages(jwt, consultationId, page, size);
    }

    @PostMapping("/{consultationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationMessageResponse sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendDoctorMessage(jwt, consultationId, request);
    }

    @PostMapping("/{consultationId}/start")
    public ConsultationResponse start(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID consultationId) {
        return consultationService.start(jwt, consultationId);
    }

    @PostMapping("/{consultationId}/complete")
    public ConsultationResponse complete(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID consultationId) {
        return consultationService.complete(jwt, consultationId);
    }

    @GetMapping("/{consultationId}/clinical-note")
    public ClinicalNoteResponse clinicalNote(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID consultationId) {
        return consultationService.clinicalNote(jwt, consultationId);
    }

    @PutMapping("/{consultationId}/clinical-note")
    public ClinicalNoteResponse updateClinicalNote(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @Valid @RequestBody UpdateClinicalNoteRequest request) {
        return consultationService.updateClinicalNote(jwt, consultationId, request);
    }
}
