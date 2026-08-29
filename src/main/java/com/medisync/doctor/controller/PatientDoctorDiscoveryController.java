package com.medisync.doctor.controller;

import com.medisync.availability.dto.AppointmentSlotResponse;
import com.medisync.common.dto.PageResponse;
import com.medisync.doctor.dto.DoctorDetailsResponse;
import com.medisync.doctor.dto.DoctorSummaryResponse;
import com.medisync.doctor.service.PatientDoctorDiscoveryService;
import com.medisync.reference.dto.SpecializationReferenceResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient/doctors")
public class PatientDoctorDiscoveryController {

    private final PatientDoctorDiscoveryService service;

    public PatientDoctorDiscoveryController(PatientDoctorDiscoveryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<DoctorSummaryResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID hospitalId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID specializationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.search(jwt, q, hospitalId, departmentId, specializationId, page, size);
    }

    @GetMapping("/specializations")
    public List<SpecializationReferenceResponse> specializations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID hospitalId,
            @RequestParam UUID departmentId) {
        return service.availableSpecializations(jwt, hospitalId, departmentId);
    }

    @GetMapping("/{doctorId}")
    public DoctorDetailsResponse details(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID doctorId) {
        return service.details(jwt, doctorId);
    }

    @GetMapping("/{doctorId}/slots")
    public List<AppointmentSlotResponse> slots(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.availableSlots(jwt, doctorId, from, to);
    }
}
