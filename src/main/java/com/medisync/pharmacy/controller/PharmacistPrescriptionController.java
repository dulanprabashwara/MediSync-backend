package com.medisync.pharmacy.controller;

import com.medisync.pharmacy.dto.DispensePrescriptionRequest;
import com.medisync.pharmacy.dto.DispensePrescriptionResponse;
import com.medisync.pharmacy.dto.PharmacyPrescriptionVerificationResponse;
import com.medisync.pharmacy.dto.PrescriptionQrVerificationRequest;
import com.medisync.pharmacy.service.PrescriptionDispensingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pharmacist/prescriptions")
public class PharmacistPrescriptionController {

    private final PrescriptionDispensingService service;

    public PharmacistPrescriptionController(PrescriptionDispensingService service) {
        this.service = service;
    }

    @PostMapping("/verify")
    public PharmacyPrescriptionVerificationResponse verify(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody PrescriptionQrVerificationRequest request) {
        return service.verify(jwt, request.qrPayload());
    }

    @PostMapping("/dispense")
    public DispensePrescriptionResponse dispense(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody DispensePrescriptionRequest request) {
        return service.dispense(jwt, request);
    }
}
