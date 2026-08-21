package com.medisync.prescription.controller;

import com.medisync.common.dto.PageResponse;
import com.medisync.prescription.dto.PatientPrescriptionDetail;
import com.medisync.prescription.dto.PatientPrescriptionSummary;
import com.medisync.prescription.service.PrescriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patient/prescriptions")
public class PatientPrescriptionController {
    private final PrescriptionService service;

    public PatientPrescriptionController(PrescriptionService service) { this.service = service; }

    @GetMapping
    public PageResponse<PatientPrescriptionSummary> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.patientList(jwt, page, size);
    }

    @GetMapping("/{prescriptionId}")
    public PatientPrescriptionDetail details(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return service.patientDetails(jwt, prescriptionId);
    }
}
