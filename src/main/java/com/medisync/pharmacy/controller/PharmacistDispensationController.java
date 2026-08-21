package com.medisync.pharmacy.controller;

import com.medisync.common.dto.PageResponse;
import com.medisync.pharmacy.dto.DispensationHistoryDetail;
import com.medisync.pharmacy.dto.DispensationHistorySummary;
import com.medisync.pharmacy.service.PrescriptionDispensingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacist/dispensations")
public class PharmacistDispensationController {

    private final PrescriptionDispensingService service;

    public PharmacistDispensationController(PrescriptionDispensingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<DispensationHistorySummary> history(@AuthenticationPrincipal Jwt jwt,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return service.history(jwt, page, size);
    }

    @GetMapping("/{dispensationId}")
    public DispensationHistoryDetail detail(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable UUID dispensationId) {
        return service.historyDetail(jwt, dispensationId);
    }
}
