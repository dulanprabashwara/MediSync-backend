package com.medisync.admin.service;

import com.medisync.audit.service.AuditService;
import com.medisync.exception.ResourceConflictException;
import com.medisync.media.MediaUrlService;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserAccountBan;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.repository.UserAccountBanRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock UserAccountBanRepository banRepository;
    @Mock DoctorProfileRepository doctorRepository;
    @Mock PatientProfileRepository patientRepository;
    @Mock PharmacistProfileRepository pharmacistRepository;
    @Mock CurrentUserService currentUserService;
    @Mock MediaUrlService mediaUrlService;
    @Mock AuditService auditService;
    @Mock JdbcTemplate jdbcTemplate;

    private AdminUserService service;
    private Jwt jwt;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, banRepository, doctorRepository, patientRepository,
                pharmacistRepository, currentUserService, mediaUrlService, auditService, jdbcTemplate);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
    }

    @Test
    void administratorAccountCannotBeBanned() {
        when(currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE)).thenReturn(admin);
        when(userRepository.findByIdForUpdate(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.ban(jwt, admin.getId(), "Not allowed"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Administrator");
        verify(banRepository, never()).saveAndFlush(any());
    }

    @Test
    void banStoresReasonAndUnbanRestoresPendingStatusWithoutDeletingHistory() {
        AppUser doctor = user(UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION);
        AtomicReference<UserAccountBan> storedBan = new AtomicReference<>();
        when(currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE)).thenReturn(admin);
        when(userRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.of(doctor));
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(banRepository.findByUserIdAndUnbannedAtIsNull(doctor.getId()))
                .thenAnswer(invocation -> Optional.ofNullable(storedBan.get()));
        when(banRepository.saveAndFlush(any(UserAccountBan.class))).thenAnswer(invocation -> {
            storedBan.set(invocation.getArgument(0));
            return storedBan.get();
        });
        when(banRepository.findByUserIdOrderByBannedAtDesc(doctor.getId()))
                .thenAnswer(invocation -> storedBan.get() == null ? List.of() : List.of(storedBan.get()));
        when(doctorRepository.findByUserId(doctor.getId())).thenReturn(Optional.empty());
        when(auditService.recentForUser(doctor.getId(), 10)).thenReturn(List.of());

        service.ban(jwt, doctor.getId(), "Professional verification policy violation");
        assertThat(doctor.getStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(storedBan.get().getReason()).isEqualTo("Professional verification policy violation");
        assertThat(storedBan.get().getPreviousStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);

        service.unban(jwt, doctor.getId());
        assertThat(doctor.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(storedBan.get().getUnbannedAt()).isNotNull();
        assertThat(storedBan.get().getUnbannedBy()).isEqualTo(admin.getId());
        assertThat(banRepository.findByUserIdOrderByBannedAtDesc(doctor.getId())).hasSize(1);
    }

    private AppUser user(UserRole role, AccountStatus status) {
        return new AppUser(UUID.randomUUID(), role.name().toLowerCase() + "@example.com", "Test", role.name(),
                null, role, status);
    }
}
