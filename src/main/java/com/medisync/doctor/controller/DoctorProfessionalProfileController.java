package com.medisync.doctor.controller;

import com.medisync.doctor.dto.DoctorProfileResponse;
import com.medisync.doctor.dto.DoctorProfileUpdateRequest;
import com.medisync.doctor.service.DoctorProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor/profile")
public class DoctorProfessionalProfileController {

    private final DoctorProfileService service;

    public DoctorProfessionalProfileController(DoctorProfileService service) {
        this.service = service;
    }

    @GetMapping
    public DoctorProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(jwt);
    }

    @PutMapping
    public DoctorProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody DoctorProfileUpdateRequest request) {
        return service.updateProfile(jwt, request);
    }

    @PostMapping("/submit-verification")
    public DoctorProfileResponse submit(@AuthenticationPrincipal Jwt jwt) {
        return service.submitForVerification(jwt);
    }
}

