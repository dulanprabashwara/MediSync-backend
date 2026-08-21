package com.medisync.pharmacy.controller;

import com.medisync.pharmacy.dto.AdminPharmacistReviewResponse;
import com.medisync.pharmacy.dto.RejectPharmacistRequest;
import com.medisync.pharmacy.service.AdminPharmacistVerificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pharmacists")
public class AdminPharmacistVerificationController {

    private final AdminPharmacistVerificationService service;

    public AdminPharmacistVerificationController(AdminPharmacistVerificationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<AdminPharmacistReviewResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        return service.pendingPharmacists(jwt);
    }

    @GetMapping("/{pharmacistId}")
    public AdminPharmacistReviewResponse pharmacist(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID pharmacistId) {
        return service.pharmacist(jwt, pharmacistId);
    }

    @PostMapping("/{pharmacistId}/verify")
    public AdminPharmacistReviewResponse verify(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID pharmacistId) {
        return service.verify(jwt, pharmacistId);
    }

    @PostMapping("/{pharmacistId}/reject")
    public AdminPharmacistReviewResponse reject(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID pharmacistId,
                                                @Valid @RequestBody RejectPharmacistRequest request) {
        return service.reject(jwt, pharmacistId, request.reason());
    }
}
