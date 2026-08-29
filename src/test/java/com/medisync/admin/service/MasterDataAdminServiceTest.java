package com.medisync.admin.service;

import com.medisync.admin.dto.DepartmentRequest;
import com.medisync.admin.dto.SpecializationRequest;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MasterDataAdminServiceTest {

    @Test
    void departmentRequiresExistingHospital() {
        CurrentUserService currentUsers = mock(CurrentUserService.class);
        HospitalRepository hospitals = mock(HospitalRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        SpecializationRepository specializations = mock(SpecializationRepository.class);
        MasterDataAdminService service = new MasterDataAdminService(currentUsers, hospitals, departments,
                specializations);
        AppUser admin = new AppUser(UUID.randomUUID(), "admin@example.com", "Admin", "User", null,
                UserRole.ADMIN, AccountStatus.ACTIVE);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "ES256")
                .subject(admin.getAuthUserId().toString()).build();
        UUID missingHospital = UUID.randomUUID();
        when(currentUsers.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(admin);
        when(hospitals.findById(missingHospital)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDepartment(jwt,
                new DepartmentRequest(missingHospital, "Cardiology", true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hospital");
    }

    @Test
    void specializationIsScopedToAValidatedHospitalDepartment() {
        CurrentUserService currentUsers = mock(CurrentUserService.class);
        HospitalRepository hospitals = mock(HospitalRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        SpecializationRepository specializations = mock(SpecializationRepository.class);
        MasterDataAdminService service = new MasterDataAdminService(currentUsers, hospitals, departments,
                specializations);
        AppUser admin = new AppUser(UUID.randomUUID(), "admin@example.com", "Admin", "User", null,
                UserRole.ADMIN, AccountStatus.ACTIVE);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "ES256")
                .subject(admin.getAuthUserId().toString()).build();
        Hospital hospital = new Hospital("Teaching Hospital", null, null, null, true);
        Department department = new Department(hospital.getId(), "Cardiology", true);
        when(currentUsers.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(admin);
        when(hospitals.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departments.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializations.saveAndFlush(any(Specialization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createSpecialization(jwt, new SpecializationRequest(
                hospital.getId(), department.getId(), "Interventional Cardiology", null, true));

        assertThat(response.hospitalId()).isEqualTo(hospital.getId());
        assertThat(response.departmentId()).isEqualTo(department.getId());
        assertThat(response.departmentName()).isEqualTo("Cardiology");
    }
}
