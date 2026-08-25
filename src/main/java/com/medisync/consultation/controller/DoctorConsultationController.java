package com.medisync.consultation.controller;

import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.dto.ClinicalNoteResponse;
import com.medisync.consultation.dto.ConsultationMessageResponse;
import com.medisync.consultation.dto.ConsultationResponse;
import com.medisync.consultation.dto.SendMessageRequest;
import com.medisync.consultation.dto.UpdateClinicalNoteRequest;
import com.medisync.consultation.dto.VideoTokenResponse;
import com.medisync.consultation.service.ConsultationChatService;
import com.medisync.consultation.service.ConsultationService;
import com.medisync.consultation.service.VideoConsultationService;
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
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doctor/consultations")
public class DoctorConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationChatService chatService;
    private final VideoConsultationService videoService;

    public DoctorConsultationController(ConsultationService consultationService,
                                        ConsultationChatService chatService,
                                        VideoConsultationService videoService) {
        this.consultationService = consultationService;
        this.chatService = chatService;
        this.videoService = videoService;
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

    @PostMapping(value = "/{consultationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationMessageResponse sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendDoctorMessage(jwt, consultationId, request);
    }

    @PostMapping(value = "/{consultationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationMessageResponse sendMediaMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return chatService.sendDoctorMedia(jwt, consultationId, content, images);
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

    @org.springframework.web.bind.annotation.DeleteMapping("/{consultationId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId,
            @PathVariable UUID messageId) {
        chatService.deleteDoctorMessage(jwt, consultationId, messageId);
    }

    // ── Video Consultation ──────────────────────────────────────────────

    @PostMapping("/{consultationId}/video/start")
    public VideoTokenResponse startVideo(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID consultationId) {
        return videoService.doctorStartVideo(jwt, consultationId);
    }

    @PostMapping("/{consultationId}/video/rejoin")
    public VideoTokenResponse rejoinVideo(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID consultationId) {
        return videoService.doctorRejoinVideo(jwt, consultationId);
    }


    @GetMapping("/{consultationId}/video/status")
    public java.util.Map<String, Boolean> videoStatus(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable UUID consultationId) {
        // Access check
        consultationService.doctorDetails(jwt, consultationId);
        return java.util.Map.of("active", videoService.isVideoActive(consultationId));
    }
}
