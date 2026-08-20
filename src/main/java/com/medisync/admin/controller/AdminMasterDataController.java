package com.medisync.admin.controller;

import com.medisync.admin.dto.DepartmentRequest;
import com.medisync.admin.dto.DepartmentResponse;
import com.medisync.admin.dto.HospitalRequest;
import com.medisync.admin.dto.HospitalResponse;
import com.medisync.admin.dto.SpecializationRequest;
import com.medisync.admin.dto.SpecializationResponse;
import com.medisync.admin.service.MasterDataAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminMasterDataController {

    private final MasterDataAdminService service;

    public AdminMasterDataController(MasterDataAdminService service) {
        this.service = service;
    }

    @GetMapping("/hospitals")
    public List<HospitalResponse> hospitals(@AuthenticationPrincipal Jwt jwt) {
        return service.hospitals(jwt);
    }

    @PostMapping("/hospitals")
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalResponse createHospital(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody HospitalRequest request) {
        return service.createHospital(jwt, request);
    }

    @PutMapping("/hospitals/{id}")
    public HospitalResponse updateHospital(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody HospitalRequest request) {
        return service.updateHospital(jwt, id, request);
    }

    @GetMapping("/departments")
    public List<DepartmentResponse> departments(@AuthenticationPrincipal Jwt jwt) {
        return service.departments(jwt);
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody DepartmentRequest request) {
        return service.createDepartment(jwt, request);
    }

    @PutMapping("/departments/{id}")
    public DepartmentResponse updateDepartment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @Valid @RequestBody DepartmentRequest request) {
        return service.updateDepartment(jwt, id, request);
    }

    @GetMapping("/specializations")
    public List<SpecializationResponse> specializations(@AuthenticationPrincipal Jwt jwt) {
        return service.specializations(jwt);
    }

    @PostMapping("/specializations")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecializationResponse createSpecialization(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody SpecializationRequest request) {
        return service.createSpecialization(jwt, request);
    }

    @PutMapping("/specializations/{id}")
    public SpecializationResponse updateSpecialization(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                       @Valid @RequestBody SpecializationRequest request) {
        return service.updateSpecialization(jwt, id, request);
    }
}

