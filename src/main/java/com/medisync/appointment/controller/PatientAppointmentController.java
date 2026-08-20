package com.medisync.appointment.controller;

import com.medisync.appointment.dto.AppointmentResponse;
import com.medisync.appointment.dto.CancelAppointmentRequest;
import com.medisync.appointment.dto.CreateAppointmentRequest;
import com.medisync.appointment.service.PatientAppointmentService;
import com.medisync.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/patient/appointments")
public class PatientAppointmentController {

    private final PatientAppointmentService service;

    public PatientAppointmentController(PatientAppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AppointmentResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(jwt, page, size);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse details(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId) {
        return service.details(jwt, appointmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CreateAppointmentRequest request) {
        return service.create(jwt, request);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID appointmentId,
                                      @Valid @RequestBody(required = false) CancelAppointmentRequest request) {
        return service.cancel(jwt, appointmentId, request);
    }
}
