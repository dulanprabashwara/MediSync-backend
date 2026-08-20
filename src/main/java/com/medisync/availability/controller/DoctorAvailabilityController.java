package com.medisync.availability.controller;

import com.medisync.availability.dto.AppointmentSlotResponse;
import com.medisync.availability.dto.AvailabilityWindowResponse;
import com.medisync.availability.dto.CreateAvailabilityRequest;
import com.medisync.availability.service.DoctorAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/availability")
public class DoctorAvailabilityController {

    private final DoctorAvailabilityService service;

    public DoctorAvailabilityController(DoctorAvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    public List<AvailabilityWindowResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(jwt);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityWindowResponse create(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateAvailabilityRequest request) {
        return service.create(jwt, request);
    }

    @PatchMapping("/{windowId}/deactivate")
    public AvailabilityWindowResponse deactivate(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID windowId) {
        return service.deactivate(jwt, windowId);
    }

    @PostMapping("/slots/{slotId}/block")
    public AppointmentSlotResponse block(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID slotId) {
        return service.block(jwt, slotId);
    }

    @PostMapping("/slots/{slotId}/unblock")
    public AppointmentSlotResponse unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID slotId) {
        return service.unblock(jwt, slotId);
    }
}
