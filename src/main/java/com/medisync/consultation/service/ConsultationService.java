package com.medisync.consultation.service;

import com.medisync.consultation.dto.ClinicalNoteResponse;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.consultation.dto.ConsultationResponse;
import com.medisync.consultation.dto.UpdateClinicalNoteRequest;
import com.medisync.consultation.entity.ConsultationClinicalNote;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.repository.ConsultationClinicalNoteRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConsultationService {

    private final ConsultationAccessService accessService;
    private final ConsultationResponseMapper responseMapper;
    private final ConsultationClinicalNoteRepository clinicalNoteRepository;
    private final ConsultationRealtimePublisher realtimePublisher;
    private final PrescriptionRepository prescriptionRepository;

    public ConsultationService(ConsultationAccessService accessService,
                               ConsultationResponseMapper responseMapper,
                               ConsultationClinicalNoteRepository clinicalNoteRepository,
                               ConsultationRealtimePublisher realtimePublisher,
                               PrescriptionRepository prescriptionRepository) {
        this.accessService = accessService;
        this.responseMapper = responseMapper;
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.realtimePublisher = realtimePublisher;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional
    public ConsultationResponse patientDetails(Jwt jwt, UUID consultationId) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requirePatient(jwt, consultationId, true);
        autoStartIfDue(context);
        return responseMapper.toResponse(context);
    }

    @Transactional
    public ConsultationResponse doctorDetails(Jwt jwt, UUID consultationId) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, true);
        autoStartIfDue(context);
        return responseMapper.toResponse(context);
    }

    @Transactional
    public ConsultationResponse start(Jwt jwt, UUID consultationId) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, true);
        context.consultation().start();
        realtimePublisher.publishAfterCommit(context.appointment(),
                ConsultationEvent.statusChanged(consultationId, context.consultation().getStatus()));
        return responseMapper.toResponse(context);
    }

    private void autoStartIfDue(ConsultationAccessService.ConsultationContext context) {
        if (context.consultation().getStatus() != ConsultationStatus.SCHEDULED) return;
        if (java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                .isBefore(context.appointment().getScheduledStart())) return;
        context.consultation().start();
        realtimePublisher.publishAfterCommit(context.appointment(),
                ConsultationEvent.statusChanged(context.consultation().getId(),
                        context.consultation().getStatus()));
    }

    @Transactional
    public ConsultationResponse complete(Jwt jwt, UUID consultationId) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, true);
        if (prescriptionRepository.existsByConsultationIdAndStatus(consultationId, PrescriptionStatus.DRAFT)) {
            throw new ResourceConflictException(
                    "Discard or issue the draft prescription before completing this consultation");
        }
        context.consultation().complete();
        realtimePublisher.publishAfterCommit(context.appointment(),
                ConsultationEvent.statusChanged(consultationId, context.consultation().getStatus()));
        return responseMapper.toResponse(context);
    }

    @Transactional(readOnly = true)
    public ClinicalNoteResponse clinicalNote(Jwt jwt, UUID consultationId) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, false);
        ConsultationClinicalNote note = clinicalNoteRepository.findByConsultationId(consultationId).orElse(null);
        requireNoteOwnership(context, note);
        return noteResponse(context, note);
    }

    @Transactional
    public ClinicalNoteResponse updateClinicalNote(Jwt jwt, UUID consultationId,
                                                   UpdateClinicalNoteRequest request) {
        ConsultationAccessService.ConsultationContext context =
                accessService.requireDoctor(jwt, consultationId, true);
        if (!context.consultation().getStatus().allowsClinicalNoteEditing()) {
            throw new ResourceConflictException(
                    "The clinical note is read-only after completion or cancellation");
        }
        ConsultationClinicalNote note = clinicalNoteRepository.findByConsultationIdForUpdate(consultationId)
                .orElseGet(() -> new ConsultationClinicalNote(
                        consultationId, context.doctor().getId(), normalizeNote(request.noteText())));
        requireNoteOwnership(context, note);
        note.update(normalizeNote(request.noteText()));
        note = clinicalNoteRepository.save(note);
        return noteResponse(context, note);
    }

    private void requireNoteOwnership(ConsultationAccessService.ConsultationContext context,
                                      ConsultationClinicalNote note) {
        if (note != null && !context.doctor().getId().equals(note.getDoctorId())) {
            throw new ResourceConflictException("Clinical note ownership is inconsistent");
        }
    }

    private ClinicalNoteResponse noteResponse(ConsultationAccessService.ConsultationContext context,
                                              ConsultationClinicalNote note) {
        return new ClinicalNoteResponse(
                context.consultation().getId(),
                note == null ? "" : note.getNoteText(),
                context.consultation().getStatus() == ConsultationStatus.COMPLETED,
                note == null ? null : note.getCreatedAt(),
                note == null ? null : note.getUpdatedAt());
    }

    private String normalizeNote(String value) {
        return value == null ? "" : value.trim();
    }
}
