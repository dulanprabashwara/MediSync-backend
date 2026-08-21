package com.medisync.prescription.controller;

import com.medisync.prescription.dto.DoctorPrescriptionResponse;
import com.medisync.prescription.service.PrescriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/consultations/{consultationId}/prescriptions")
public class DoctorConsultationPrescriptionController {
    private final PrescriptionService service;

    public DoctorConsultationPrescriptionController(PrescriptionService service) { this.service = service; }

    @GetMapping
    public List<DoctorPrescriptionResponse> list(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId) {
        return service.consultationList(jwt, consultationId);
    }

    @PostMapping
    public DoctorPrescriptionResponse create(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID consultationId) {
        return service.createDraft(jwt, consultationId);
    }
}
