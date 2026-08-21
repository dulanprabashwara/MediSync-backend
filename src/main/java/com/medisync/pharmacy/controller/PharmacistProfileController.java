package com.medisync.pharmacy.controller;

import com.medisync.pharmacy.dto.PharmacistProfileResponse;
import com.medisync.pharmacy.dto.PharmacistProfileUpdateRequest;
import com.medisync.pharmacy.service.PharmacistProfileService;
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
@RequestMapping("/api/pharmacist/professional-profile")
public class PharmacistProfileController {

    private final PharmacistProfileService service;

    public PharmacistProfileController(PharmacistProfileService service) {
        this.service = service;
    }

    @GetMapping
    public PharmacistProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(jwt);
    }

    @PutMapping
    public PharmacistProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody PharmacistProfileUpdateRequest request) {
        return service.updateProfile(jwt, request);
    }

    @PostMapping("/submit-verification")
    public PharmacistProfileResponse submit(@AuthenticationPrincipal Jwt jwt) {
        return service.submitForVerification(jwt);
    }
}
