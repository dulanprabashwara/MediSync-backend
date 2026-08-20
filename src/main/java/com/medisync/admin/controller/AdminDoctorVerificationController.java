package com.medisync.admin.controller;

import com.medisync.admin.dto.AdminDoctorReviewResponse;
import com.medisync.admin.dto.RejectDoctorRequest;
import com.medisync.admin.service.AdminDoctorVerificationService;
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
@RequestMapping("/api/admin/doctors")
public class AdminDoctorVerificationController {

    private final AdminDoctorVerificationService service;

    public AdminDoctorVerificationController(AdminDoctorVerificationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<AdminDoctorReviewResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        return service.pendingDoctors(jwt);
    }

    @GetMapping("/{doctorId}")
    public AdminDoctorReviewResponse doctor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID doctorId) {
        return service.doctor(jwt, doctorId);
    }

    @PostMapping("/{doctorId}/verify")
    public AdminDoctorReviewResponse verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID doctorId) {
        return service.verify(jwt, doctorId);
    }

    @PostMapping("/{doctorId}/reject")
    public AdminDoctorReviewResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID doctorId,
                                            @Valid @RequestBody RejectDoctorRequest request) {
        return service.reject(jwt, doctorId, request.reason());
    }
}

