package com.medisync.reference.service;

import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceDataServiceTest {

    @Test
    void normalReferenceEndpointsReturnOnlyRepositoryActiveResults() {
        HospitalRepository hospitals = mock(HospitalRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        SpecializationRepository specializations = mock(SpecializationRepository.class);
        ReferenceDataService service = new ReferenceDataService(hospitals, departments, specializations);
        Hospital activeHospital = new Hospital("Active Hospital", null, null, null, true);
        Specialization activeSpecialization = new Specialization("Cardiology", null, true);
        when(hospitals.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(activeHospital));
        when(specializations.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(activeSpecialization));

        assertThat(service.hospitals()).extracting("name").containsExactly("Active Hospital");
        assertThat(service.specializations()).extracting("name").containsExactly("Cardiology");
    }

    @Test
    void scopedSpecializationsRequireDepartmentToBelongToHospital() {
        HospitalRepository hospitals = mock(HospitalRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        SpecializationRepository specializations = mock(SpecializationRepository.class);
        ReferenceDataService service = new ReferenceDataService(hospitals, departments, specializations);
        Hospital hospital = new Hospital("Teaching Hospital", null, null, null, true);
        Department department = new Department(hospital.getId(), "Cardiology", true);
        Specialization specialization = new Specialization(
                department.getId(), "Interventional Cardiology", null, true);
        when(hospitals.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departments.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializations.findActiveForDepartment(department.getId())).thenReturn(List.of(specialization));

        assertThat(service.specializations(hospital.getId(), department.getId()))
                .extracting("name")
                .containsExactly("Interventional Cardiology");
    }
}
