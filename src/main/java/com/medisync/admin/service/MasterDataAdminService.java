package com.medisync.admin.service;

import com.medisync.admin.dto.DepartmentRequest;
import com.medisync.admin.dto.DepartmentResponse;
import com.medisync.admin.dto.HospitalRequest;
import com.medisync.admin.dto.HospitalResponse;
import com.medisync.admin.dto.SpecializationRequest;
import com.medisync.admin.dto.SpecializationResponse;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.UserRole;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MasterDataAdminService {

    private final CurrentUserService currentUserService;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public MasterDataAdminService(
            CurrentUserService currentUserService,
            HospitalRepository hospitalRepository,
            DepartmentRepository departmentRepository,
            SpecializationRepository specializationRepository
    ) {
        this.currentUserService = currentUserService;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> hospitals(Jwt jwt) {
        requireAdmin(jwt);
        return hospitalRepository.findAllByOrderByNameAsc().stream().map(HospitalResponse::from).toList();
    }

    @Transactional
    public HospitalResponse createHospital(Jwt jwt, HospitalRequest request) {
        requireAdmin(jwt);
        String name = request.name().trim();
        if (hospitalRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("A hospital with this name already exists");
        }
        Hospital hospital = new Hospital(name, optional(request.addressLine()), optional(request.city()),
                optional(request.phone()), request.active() == null || request.active());
        return HospitalResponse.from(hospitalRepository.saveAndFlush(hospital));
    }

    @Transactional
    public HospitalResponse updateHospital(Jwt jwt, UUID id, HospitalRequest request) {
        requireAdmin(jwt);
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
        String name = request.name().trim();
        if (hospitalRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("A hospital with this name already exists");
        }
        hospital.update(name, optional(request.addressLine()), optional(request.city()), optional(request.phone()),
                request.active() == null ? hospital.isActive() : request.active());
        return HospitalResponse.from(hospitalRepository.saveAndFlush(hospital));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> departments(Jwt jwt) {
        requireAdmin(jwt);
        Map<UUID, Hospital> hospitals = hospitalRepository.findAll().stream()
                .collect(Collectors.toMap(Hospital::getId, Function.identity()));
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(department -> DepartmentResponse.from(department,
                        hospitals.containsKey(department.getHospitalId())
                                ? hospitals.get(department.getHospitalId()).getName()
                                : "Unknown hospital"))
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(Jwt jwt, DepartmentRequest request) {
        requireAdmin(jwt);
        Hospital hospital = requireHospital(request.hospitalId());
        String name = request.name().trim();
        if (departmentRepository.existsByHospitalIdAndNameIgnoreCase(hospital.getId(), name)) {
            throw new ResourceConflictException("This hospital already has a department with that name");
        }
        Department department = new Department(hospital.getId(), name,
                request.active() == null || request.active());
        return DepartmentResponse.from(departmentRepository.saveAndFlush(department), hospital.getName());
    }

    @Transactional
    public DepartmentResponse updateDepartment(Jwt jwt, UUID id, DepartmentRequest request) {
        requireAdmin(jwt);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Hospital hospital = requireHospital(request.hospitalId());
        String name = request.name().trim();
        if (departmentRepository.existsByHospitalIdAndNameIgnoreCaseAndIdNot(hospital.getId(), name, id)) {
            throw new ResourceConflictException("This hospital already has a department with that name");
        }
        department.update(hospital.getId(), name,
                request.active() == null ? department.isActive() : request.active());
        return DepartmentResponse.from(departmentRepository.saveAndFlush(department), hospital.getName());
    }

    @Transactional(readOnly = true)
    public List<SpecializationResponse> specializations(Jwt jwt) {
        requireAdmin(jwt);
        return specializationRepository.findAllByOrderByNameAsc().stream()
                .map(SpecializationResponse::from)
                .toList();
    }

    @Transactional
    public SpecializationResponse createSpecialization(Jwt jwt, SpecializationRequest request) {
        requireAdmin(jwt);
        String name = request.name().trim();
        if (specializationRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("A specialization with this name already exists");
        }
        Specialization specialization = new Specialization(name, optional(request.description()),
                request.active() == null || request.active());
        return SpecializationResponse.from(specializationRepository.saveAndFlush(specialization));
    }

    @Transactional
    public SpecializationResponse updateSpecialization(Jwt jwt, UUID id, SpecializationRequest request) {
        requireAdmin(jwt);
        Specialization specialization = specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
        String name = request.name().trim();
        if (specializationRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("A specialization with this name already exists");
        }
        specialization.update(name, optional(request.description()),
                request.active() == null ? specialization.isActive() : request.active());
        return SpecializationResponse.from(specializationRepository.saveAndFlush(specialization));
    }

    private void requireAdmin(Jwt jwt) {
        currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE);
    }

    private Hospital requireHospital(UUID id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

