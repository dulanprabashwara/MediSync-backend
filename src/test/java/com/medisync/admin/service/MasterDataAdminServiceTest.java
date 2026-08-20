package com.medisync.admin.service;

import com.medisync.admin.dto.DepartmentRequest;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.repository.HospitalRepository;
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
}

