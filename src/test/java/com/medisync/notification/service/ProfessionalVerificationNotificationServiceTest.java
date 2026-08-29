package com.medisync.notification.service;

import com.medisync.notification.NotificationType;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfessionalVerificationNotificationServiceTest {

    private NotificationService notificationService;
    private AppUserRepository users;
    private ProfessionalVerificationNotificationService service;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        users = mock(AppUserRepository.class);
        service = new ProfessionalVerificationNotificationService(notificationService, users);
        admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        when(users.findByRoleAndStatus(UserRole.ADMIN, AccountStatus.ACTIVE)).thenReturn(List.of(admin));
    }

    @Test
    void doctorSubmissionNotifiesActiveAdminUsingAppUserIds() {
        AppUser doctor = user(UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION);
        UUID profileId = UUID.randomUUID();

        service.submitted(doctor, profileId, false, OffsetDateTime.now());

        verify(notificationService).createNotification(eq(admin.getId()), eq(doctor.getId()),
                eq(NotificationType.DOCTOR_VERIFICATION_SUBMITTED), eq("Doctor verification submitted"),
                eq(doctor.getFirstName() + " " + doctor.getLastName()
                        + " submitted professional credentials for review."),
                eq("/admin/verification"), eq("DOCTOR_PROFILE"), eq(profileId), anyString());
    }

    @Test
    void pharmacistResubmissionUsesResubmissionType() {
        AppUser pharmacist = user(UserRole.PHARMACIST, AccountStatus.PENDING_VERIFICATION);
        UUID profileId = UUID.randomUUID();

        service.submitted(pharmacist, profileId, true, OffsetDateTime.now());

        verify(notificationService).createNotification(eq(admin.getId()), eq(pharmacist.getId()),
                eq(NotificationType.PHARMACIST_VERIFICATION_RESUBMITTED),
                eq("Pharmacist verification resubmitted"),
                eq(pharmacist.getFirstName() + " " + pharmacist.getLastName()
                        + " submitted professional credentials for review."),
                eq("/admin/verification"), eq("PHARMACIST_PROFILE"), eq(profileId), anyString());
    }

    @Test
    void approvalNotifiesProfessionalAppUser() {
        AppUser doctor = user(UserRole.DOCTOR, AccountStatus.ACTIVE);
        UUID profileId = UUID.randomUUID();

        service.reviewed(admin, doctor, profileId, true, OffsetDateTime.now());

        verify(notificationService).createNotification(eq(doctor.getId()), eq(admin.getId()),
                eq(NotificationType.VERIFICATION_APPROVED), eq("Professional verification approved"),
                eq("Your professional verification was approved. Your professional workspace is now available."),
                eq("/doctor/profile"), eq("DOCTOR_PROFILE"), eq(profileId), anyString());
    }

    private AppUser user(UserRole role, AccountStatus status) {
        return new AppUser(UUID.randomUUID(), role.name().toLowerCase() + "@example.com",
                "Test", role.name(), null, role, status);
    }
}
