package com.medisync.security;

import com.medisync.common.HealthController;
import com.medisync.admin.controller.AdminDoctorVerificationController;
import com.medisync.admin.controller.AdminMasterDataController;
import com.medisync.admin.service.AdminDoctorVerificationService;
import com.medisync.admin.service.MasterDataAdminService;
import com.medisync.config.CorsProperties;
import com.medisync.config.SecurityProperties;
import com.medisync.user.controller.RoleProfileController;
import com.medisync.user.controller.UserController;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HealthController.class, UserController.class, RoleProfileController.class,
        AdminMasterDataController.class, AdminDoctorVerificationController.class})
@Import(SecurityConfig.class)
@EnableConfigurationProperties({SecurityProperties.class, CorsProperties.class})
@TestPropertySource(properties = {
        "medisync.security.supabase-url=https://example.supabase.co",
        "medisync.security.jwks-url=https://example.supabase.co/auth/v1/.well-known/jwks.json",
        "medisync.security.audience=authenticated",
        "medisync.cors.frontend-url=http://localhost:3000"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MasterDataAdminService masterDataAdminService;

    @MockitoBean
    private AdminDoctorVerificationService adminDoctorVerificationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void currentUserEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void databaseRolePreventsCrossRoleAccess() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "patient@example.com", "John", "Silva", null,
                        UserRole.PATIENT, AccountStatus.ACTIVE)));

        mockMvc.perform(get("/api/doctor/profile")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void patientCannotAccessAdminMasterData() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "patient@example.com", "John", "Silva", null,
                        UserRole.PATIENT, AccountStatus.ACTIVE)));

        mockMvc.perform(get("/api/admin/hospitals")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void doctorCannotCallAdminVerificationEndpoint() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "doctor@example.com", "Nimal", "Perera", null,
                        UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION)));

        mockMvc.perform(post("/api/admin/doctors/{doctorId}/verify", UUID.randomUUID())
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void activeAdminCanCreateHospital() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "admin@example.com", "Admin", "User", null,
                        UserRole.ADMIN, AccountStatus.ACTIVE)));

        mockMvc.perform(post("/api/admin/hospitals")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Central Hospital\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void doctorCannotUsePatientBookingEndpoint() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "doctor@example.com", "Nimal", "Perera", null,
                        UserRole.DOCTOR, AccountStatus.ACTIVE)));

        mockMvc.perform(post("/api/patient/appointments")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pharmacistCannotUsePatientOrDoctorAppointmentEndpoints() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "pharmacist@example.com", "Ravi", "Fernando", null,
                        UserRole.PHARMACIST, AccountStatus.ACTIVE)));

        mockMvc.perform(get("/api/patient/doctors")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctor/appointments")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotBypassPatientBookingWorkflow() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "admin@example.com", "Admin", "User", null,
                        UserRole.ADMIN, AccountStatus.ACTIVE)));

        mockMvc.perform(post("/api/patient/appointments")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotManageDoctorAvailabilityOrRequests() throws Exception {
        UUID authUserId = UUID.randomUUID();
        when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                new AppUser(authUserId, "patient@example.com", "John", "Silva", null,
                        UserRole.PATIENT, AccountStatus.ACTIVE)));

        mockMvc.perform(get("/api/doctor/availability")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctor/appointments")
                        .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientPharmacistAndAdminCannotAccessDoctorClinicalNotes() throws Exception {
        UUID consultationId = UUID.randomUUID();
        for (UserRole role : new UserRole[]{UserRole.PATIENT, UserRole.PHARMACIST, UserRole.ADMIN}) {
            UUID authUserId = UUID.randomUUID();
            when(appUserRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(
                    new AppUser(authUserId, role.name().toLowerCase() + "@example.com", "Test", role.name(), null,
                            role, AccountStatus.ACTIVE)));

            mockMvc.perform(get("/api/doctor/consultations/{consultationId}/clinical-note", consultationId)
                            .with(jwt().jwt(token -> token.subject(authUserId.toString()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
    }
}
