package com.medisync.appointment.controller;

import com.medisync.appointment.dto.AppointmentResponse;
import com.medisync.appointment.dto.RequiredReasonRequest;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.service.DoctorAppointmentService;
import com.medisync.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/appointments")
public class DoctorAppointmentController {

    private final DoctorAppointmentService service;

    public DoctorAppointmentController(DoctorAppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AppointmentResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(required = false) AppointmentStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(jwt, status, page, size);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse details(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId) {
        return service.details(jwt, appointmentId);
    }

    @PostMapping("/{appointmentId}/accept")
    public AppointmentResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId) {
        return service.accept(jwt, appointmentId);
    }

    @PostMapping("/{appointmentId}/reject")
    public AppointmentResponse reject(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID appointmentId,
                                      @Valid @RequestBody RequiredReasonRequest request) {
        return service.reject(jwt, appointmentId, request.reason());
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID appointmentId,
                                      @Valid @RequestBody RequiredReasonRequest request) {
        return service.cancel(jwt, appointmentId, request.reason());
    }
}
