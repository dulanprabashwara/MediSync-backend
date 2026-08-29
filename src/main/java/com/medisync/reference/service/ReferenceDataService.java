package com.medisync.reference.service;

import com.medisync.department.repository.DepartmentRepository;
import com.medisync.department.entity.Department;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.reference.dto.DepartmentReferenceResponse;
import com.medisync.reference.dto.HospitalReferenceResponse;
import com.medisync.reference.dto.SpecializationReferenceResponse;
import com.medisync.specialization.repository.SpecializationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReferenceDataService {

    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public ReferenceDataService(HospitalRepository hospitalRepository,
                                DepartmentRepository departmentRepository,
                                SpecializationRepository specializationRepository) {
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    @Transactional(readOnly = true)
    public List<HospitalReferenceResponse> hospitals() {
        return hospitalRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(hospital -> new HospitalReferenceResponse(hospital.getId(), hospital.getName(), hospital.getCity()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentReferenceResponse> departments(UUID hospitalId) {
        hospitalRepository.findById(hospitalId)
                .filter(hospital -> hospital.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Active hospital not found"));
        return departmentRepository.findByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId).stream()
                .map(department -> new DepartmentReferenceResponse(department.getId(), department.getHospitalId(),
                        department.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecializationReferenceResponse> specializations() {
        return specializationRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(specialization -> new SpecializationReferenceResponse(specialization.getId(),
                        specialization.getName(), specialization.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecializationReferenceResponse> specializations(UUID hospitalId, UUID departmentId) {
        hospitalRepository.findById(hospitalId)
                .filter(hospital -> hospital.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Active hospital not found"));
        Department department = departmentRepository.findById(departmentId)
                .filter(candidate -> candidate.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Active department not found"));
        if (!department.getHospitalId().equals(hospitalId)) {
            throw new InvalidRequestException("The selected department does not belong to the selected hospital");
        }
        return specializationRepository.findActiveForDepartment(departmentId).stream()
                .map(specialization -> new SpecializationReferenceResponse(specialization.getId(),
                        specialization.getName(), specialization.getDescription()))
                .toList();
    }
}
