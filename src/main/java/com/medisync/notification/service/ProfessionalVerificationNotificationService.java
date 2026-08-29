package com.medisync.notification.service;

import com.medisync.notification.NotificationType;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ProfessionalVerificationNotificationService {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;

    public ProfessionalVerificationNotificationService(NotificationService notificationService,
                                                       AppUserRepository appUserRepository) {
        this.notificationService = notificationService;
        this.appUserRepository = appUserRepository;
    }

    public void submitted(AppUser professional, UUID profileId, boolean resubmission,
                          OffsetDateTime submittedAt) {
        NotificationType type = submissionType(professional.getRole(), resubmission);
        String profession = professional.getRole() == UserRole.DOCTOR ? "Doctor" : "Pharmacist";
        String title = profession + " verification " + (resubmission ? "resubmitted" : "submitted");
        String message = professional.getFirstName() + " " + professional.getLastName()
                + " submitted professional credentials for review.";
        String dedupeSuffix = submittedAt == null ? profileId.toString() : submittedAt.toString();

        for (AppUser admin : appUserRepository.findByRoleAndStatus(UserRole.ADMIN, AccountStatus.ACTIVE)) {
            notificationService.createNotification(admin.getId(), professional.getId(), type, title, message,
                    "/admin/verification", profession.toUpperCase() + "_PROFILE", profileId,
                    type.name() + ":" + profileId + ":" + dedupeSuffix + ":" + admin.getId());
        }
    }

    public void reviewed(AppUser admin, AppUser professional, UUID profileId, boolean approved,
                         OffsetDateTime submittedAt) {
        NotificationType type = approved
                ? NotificationType.VERIFICATION_APPROVED
                : NotificationType.VERIFICATION_REJECTED;
        String title = approved ? "Professional verification approved" : "Professional verification rejected";
        String message = approved
                ? "Your professional verification was approved. Your professional workspace is now available."
                : "Your professional verification requires changes. Review the reason and resubmit your profile.";
        String route = professional.getRole() == UserRole.DOCTOR ? "/doctor/profile" : "/pharmacist/profile";
        String dedupeSuffix = submittedAt == null ? profileId.toString() : submittedAt.toString();

        notificationService.createNotification(professional.getId(), admin.getId(), type, title, message, route,
                professional.getRole().name() + "_PROFILE", profileId,
                type.name() + ":" + profileId + ":" + dedupeSuffix);
    }

    private NotificationType submissionType(UserRole role, boolean resubmission) {
        if (role == UserRole.DOCTOR) {
            return resubmission
                    ? NotificationType.DOCTOR_VERIFICATION_RESUBMITTED
                    : NotificationType.DOCTOR_VERIFICATION_SUBMITTED;
        }
        if (role == UserRole.PHARMACIST) {
            return resubmission
                    ? NotificationType.PHARMACIST_VERIFICATION_RESUBMITTED
                    : NotificationType.PHARMACIST_VERIFICATION_SUBMITTED;
        }
        throw new IllegalArgumentException("Professional verification notifications require a professional role");
    }
}
