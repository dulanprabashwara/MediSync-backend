package com.medisync.prescription.controller;

import com.medisync.common.dto.PageResponse;
import com.medisync.prescription.dto.CancelPrescriptionRequest;
import com.medisync.prescription.dto.DoctorPrescriptionResponse;
import com.medisync.prescription.dto.PrescriptionDraftRequest;
import com.medisync.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/prescriptions")
public class DoctorPrescriptionController {
    private final PrescriptionService service;

    public DoctorPrescriptionController(PrescriptionService service) { this.service = service; }

    @GetMapping
    public PageResponse<DoctorPrescriptionResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.doctorList(jwt, page, size);
    }

    @GetMapping("/{prescriptionId}")
    public DoctorPrescriptionResponse details(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return service.doctorDetails(jwt, prescriptionId);
    }

    @PutMapping("/{prescriptionId}")
    public DoctorPrescriptionResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId,
            @Valid @RequestBody PrescriptionDraftRequest request) {
        return service.updateDraft(jwt, prescriptionId, request);
    }

    @PostMapping("/{prescriptionId}/issue")
    public DoctorPrescriptionResponse issue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return service.issue(jwt, prescriptionId);
    }

    @PostMapping("/{prescriptionId}/confirm-payment")
    public DoctorPrescriptionResponse confirmPayment(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable UUID prescriptionId) {
        return service.confirmDoctorFee(jwt, prescriptionId);
    }

    @PostMapping("/{prescriptionId}/cancel")
    public DoctorPrescriptionResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId,
            @Valid @RequestBody CancelPrescriptionRequest request) {
        return service.cancel(jwt, prescriptionId, request.reason());
    }

    @DeleteMapping("/{prescriptionId}")
    public void discard(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        service.discardDraft(jwt, prescriptionId);
    }
}
