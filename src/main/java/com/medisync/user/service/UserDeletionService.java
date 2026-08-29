package com.medisync.user.service;

import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.audit.AuditActions;
import com.medisync.audit.service.AuditService;
import com.medisync.exception.ResourceConflictException;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.exception.InvalidAuthenticatedUserException;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UserDeletionService {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionService.class);

    private final AppUserRepository appUserRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PharmacistProfileRepository pharmacistProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditService auditService;
    private final ProfileImageService profileImageService;
    private final SupabaseAuthDeletionService authDeletionService;

    public UserDeletionService(AppUserRepository appUserRepository,
                               DoctorProfileRepository doctorProfileRepository,
                               PharmacistProfileRepository pharmacistProfileRepository,
                               AppointmentRepository appointmentRepository,
                               PrescriptionRepository prescriptionRepository,
                               AuditService auditService,
                               ProfileImageService profileImageService,
                               SupabaseAuthDeletionService authDeletionService) {
        this.appUserRepository = appUserRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.pharmacistProfileRepository = pharmacistProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.auditService = auditService;
        this.profileImageService = profileImageService;
        this.authDeletionService = authDeletionService;
    }

    @Transactional
    public void deleteSelfAccount(Jwt jwt) {
        UUID authUserId = authenticatedUserId(jwt);
        AppUser user = appUserRepository.findByAuthUserIdForUpdate(authUserId)
                .orElseThrow(() -> new InvalidAuthenticatedUserException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Admins cannot self-delete");
        }

        performDeletion(user, user, "SELF", "User requested account deletion");
    }

    @Transactional
    public void deleteAccountByAdmin(Jwt jwt, UUID targetUserId, String reason) {
        if (reason == null || reason.trim().length() < 3) {
            throw new IllegalArgumentException("A valid deletion reason must be provided");
        }

        UUID adminAuthUserId = authenticatedUserId(jwt);
        AppUser admin = appUserRepository.findByAuthUserId(adminAuthUserId)
                .orElseThrow(() -> new InvalidAuthenticatedUserException("Admin not found"));

        if (admin.getRole() != UserRole.ADMIN || admin.getStatus() != AccountStatus.ACTIVE) {
            throw new org.springframework.security.access.AccessDeniedException("Only active admins can delete accounts");
        }

        AppUser targetUser = appUserRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResourceConflictException("Target user not found"));

        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot delete an ADMIN account");
        }

        performDeletion(targetUser, admin, "ADMIN", reason.trim());
    }

    private void performDeletion(AppUser targetUser, AppUser actor, String source, String reason) {
        if (targetUser.getStatus() == AccountStatus.DELETED) {
            throw new ResourceConflictException("Account is already deleted");
        }

        checkActiveWorkflows(targetUser);

        String deletedRole = targetUser.getRole().name();
        UUID targetId = targetUser.getId();
        UUID authUserId = targetUser.getAuthUserId();

        String anonymizedFirst = "Deleted";
        String anonymizedLast = switch (targetUser.getRole()) {
            case PATIENT -> "Patient";
            case DOCTOR -> "Doctor";
            case PHARMACIST -> "Pharmacist";
            default -> "User";
        };

        if (targetUser.getProfileImageKey() != null) {
            try {
                profileImageService.deleteProfileImage(targetUser.getProfileImageKey());
            } catch (Exception e) {
                log.warn("Failed to delete profile image for user {}: {}", targetId, e.getMessage());
            }
        }

        targetUser.delete(anonymizedFirst, anonymizedLast, actor.getId(), reason, source);
        appUserRepository.save(targetUser);

        if (targetUser.getRole() == UserRole.DOCTOR) {
            doctorProfileRepository.findByUserIdForUpdate(targetId).ifPresent(doc -> {
                doc.anonymize();
                doctorProfileRepository.save(doc);
            });
        } else if (targetUser.getRole() == UserRole.PHARMACIST) {
            pharmacistProfileRepository.findByUserIdForUpdate(targetId).ifPresent(pharm -> {
                pharm.anonymize();
                pharmacistProfileRepository.save(pharm);
            });
        }

        auditService.record(actor, AuditActions.USER_ACCOUNT_DELETED, "APP_USER", targetId, Map.of(
                "source", source,
                "role", deletedRole
        ));

        // Do not commit an application-level deletion while the login identity
        // remains active. Propagating a failure rolls this transaction back.
        authDeletionService.deleteSupabaseAuthUser(authUserId);

        log.info("Successfully deleted user account {} via {}", targetId, source);
    }

    private void checkActiveWorkflows(AppUser user) {
        if (user.getRole() == UserRole.PATIENT) {
            if (appointmentRepository.hasActiveWorkflowsForPatient(user.getId())) {
                throw new ResourceConflictException("Your account cannot be deleted while you have an active appointment or consultation.");
            }
            if (prescriptionRepository.hasActivePrescriptionsForPatient(user.getId())) {
                throw new ResourceConflictException("Your account cannot be deleted while you have an active prescription.");
            }
        } else if (user.getRole() == UserRole.DOCTOR) {
            if (appointmentRepository.hasActiveWorkflowsForDoctor(user.getId())) {
                throw new ResourceConflictException("Account cannot be deleted while you have active appointments or consultations.");
            }
            if (prescriptionRepository.hasActivePrescriptionsForDoctor(user.getId())) {
                throw new ResourceConflictException("Account cannot be deleted while you have active unresolved prescriptions.");
            }
        }
    }

    private UUID authenticatedUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidAuthenticatedUserException("The authenticated user identifier is invalid");
        }
    }
}
