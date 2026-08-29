package com.medisync.reference.controller;

import com.medisync.reference.dto.DepartmentReferenceResponse;
import com.medisync.reference.dto.HospitalReferenceResponse;
import com.medisync.reference.dto.SpecializationReferenceResponse;
import com.medisync.reference.service.ReferenceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reference")
public class ReferenceDataController {

    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/hospitals")
    public List<HospitalReferenceResponse> hospitals() {
        return service.hospitals();
    }

    @GetMapping("/hospitals/{hospitalId}/departments")
    public List<DepartmentReferenceResponse> departments(@PathVariable UUID hospitalId) {
        return service.departments(hospitalId);
    }

    @GetMapping("/specializations")
    public List<SpecializationReferenceResponse> specializations() {
        return service.specializations();
    }

    @GetMapping("/hospitals/{hospitalId}/departments/{departmentId}/specializations")
    public List<SpecializationReferenceResponse> specializations(@PathVariable UUID hospitalId,
                                                                 @PathVariable UUID departmentId) {
        return service.specializations(hospitalId, departmentId);
    }
}
