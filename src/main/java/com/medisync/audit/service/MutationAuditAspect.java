package com.medisync.audit.service;

import com.medisync.audit.AuditActions;
import com.medisync.user.entity.AppUser;
import com.medisync.user.service.CurrentUserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class MutationAuditAspect {

    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public MutationAuditAspect(CurrentUserService currentUserService, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @AfterReturning(pointcut = """
            execution(* com.medisync.user.service.UserService.onboard(..)) ||
            execution(* com.medisync.doctor.service.DoctorProfileService.submitForVerification(..)) ||
            execution(* com.medisync.pharmacy.service.PharmacistProfileService.submitForVerification(..)) ||
            execution(* com.medisync.admin.service.AdminDoctorVerificationService.verify(..)) ||
            execution(* com.medisync.admin.service.AdminDoctorVerificationService.reject(..)) ||
            execution(* com.medisync.pharmacy.service.AdminPharmacistVerificationService.verify(..)) ||
            execution(* com.medisync.pharmacy.service.AdminPharmacistVerificationService.reject(..)) ||
            execution(* com.medisync.admin.service.MasterDataAdminService.create*(..)) ||
            execution(* com.medisync.admin.service.MasterDataAdminService.update*(..)) ||
            execution(* com.medisync.availability.service.DoctorAvailabilityService.create(..)) ||
            execution(* com.medisync.availability.service.DoctorAvailabilityService.block(..)) ||
            execution(* com.medisync.availability.service.DoctorAvailabilityService.unblock(..)) ||
            execution(* com.medisync.appointment.service.PatientAppointmentService.create(..)) ||
            execution(* com.medisync.appointment.service.PatientAppointmentService.cancel(..)) ||
            execution(* com.medisync.appointment.service.DoctorAppointmentService.accept(..)) ||
            execution(* com.medisync.appointment.service.DoctorAppointmentService.reject(..)) ||
            execution(* com.medisync.appointment.service.DoctorAppointmentService.cancel(..)) ||
            execution(* com.medisync.consultation.service.ConsultationService.start(..)) ||
            execution(* com.medisync.consultation.service.ConsultationService.complete(..)) ||
            execution(* com.medisync.consultation.service.ConsultationService.updateClinicalNote(..)) ||
            execution(* com.medisync.pharmacy.service.PrescriptionDispensingService.dispense(..))
            """, returning = "result")
    public void recordSuccessfulMutation(JoinPoint joinPoint, Object result) {
        Jwt jwt = Arrays.stream(joinPoint.getArgs())
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .findFirst()
                .orElse(null);
        if (jwt == null) return;
        AppUser actor = currentUserService.requireCurrentUser(jwt);
        String service = joinPoint.getTarget().getClass().getSimpleName();
        String operation = joinPoint.getSignature().getName();
        String action = action(service, operation);
        UUID targetId = Arrays.stream(joinPoint.getArgs())
                .filter(UUID.class::isInstance)
                .map(UUID.class::cast)
                .findFirst()
                .orElseGet(() -> responseId(result));
        auditService.record(actor, action, targetType(action), targetId,
                Map.of("operation", operation));
    }

    private String action(String service, String operation) {
        if (service.contains("UserService") && operation.equals("onboard")) return AuditActions.USER_ONBOARDED;
        if (service.equals("DoctorProfileService") && operation.equals("submitForVerification")) {
            return AuditActions.DOCTOR_VERIFICATION_SUBMITTED;
        }
        if (service.equals("PharmacistProfileService") && operation.equals("submitForVerification")) {
            return AuditActions.PHARMACIST_VERIFICATION_SUBMITTED;
        }
        if (service.contains("AdminDoctorVerification")) {
            return operation.equals("verify") ? AuditActions.DOCTOR_APPROVED : AuditActions.DOCTOR_REJECTED;
        }
        if (service.contains("AdminPharmacistVerification")) {
            return operation.equals("verify") ? AuditActions.PHARMACIST_APPROVED : AuditActions.PHARMACIST_REJECTED;
        }
        if (service.contains("MasterData")) return masterDataAction(operation);
        if (service.contains("Availability")) {
            return operation.equals("create") ? AuditActions.AVAILABILITY_CREATED
                    : AuditActions.AVAILABILITY_SLOT_UPDATED;
        }
        if (service.equals("PatientAppointmentService")) {
            return operation.equals("create") ? AuditActions.APPOINTMENT_REQUESTED
                    : AuditActions.APPOINTMENT_CANCELLED;
        }
        if (service.equals("DoctorAppointmentService")) {
            return switch (operation) {
                case "accept" -> AuditActions.APPOINTMENT_ACCEPTED;
                case "reject" -> AuditActions.APPOINTMENT_REJECTED;
                default -> AuditActions.APPOINTMENT_CANCELLED;
            };
        }
        if (service.contains("Consultation")) {
            return switch (operation) {
                case "start" -> AuditActions.CONSULTATION_STARTED;
                case "complete" -> AuditActions.CONSULTATION_COMPLETED;
                default -> AuditActions.CLINICAL_NOTE_UPDATED;
            };
        }
        if (service.contains("Dispensing")) return AuditActions.PRESCRIPTION_DISPENSED;
        return "MUTATION_RECORDED";
    }

    private String masterDataAction(String operation) {
        return switch (operation) {
            case "createHospital" -> AuditActions.HOSPITAL_CREATED;
            case "updateHospital" -> AuditActions.HOSPITAL_STATUS_CHANGED;
            case "createDepartment" -> AuditActions.DEPARTMENT_CREATED;
            case "updateDepartment" -> AuditActions.DEPARTMENT_STATUS_CHANGED;
            case "createSpecialization" -> AuditActions.SPECIALIZATION_CREATED;
            default -> AuditActions.SPECIALIZATION_STATUS_CHANGED;
        };
    }

    private String targetType(String action) {
        if (action.contains("USER")) return "APP_USER";
        if (action.contains("DOCTOR")) return "DOCTOR_PROFILE";
        if (action.contains("PHARMACIST")) return "PHARMACIST_PROFILE";
        if (action.contains("HOSPITAL") || action.contains("DEPARTMENT")
                || action.contains("SPECIALIZATION")) return "REFERENCE_DATA";
        if (action.contains("AVAILABILITY")) return "AVAILABILITY";
        if (action.contains("APPOINTMENT")) return "APPOINTMENT";
        if (action.contains("CONSULTATION")) return "CONSULTATION";
        if (action.contains("DISPENSED")) return "PRESCRIPTION";
        return "RESOURCE";
    }

    private UUID responseId(Object result) {
        if (result == null) return null;
        for (String methodName : ListHolder.ID_METHODS) {
            try {
                Method method = result.getClass().getMethod(methodName);
                Object value = method.invoke(result);
                if (value instanceof UUID id) return id;
            } catch (ReflectiveOperationException ignored) {
                // Response types expose different operational identifiers.
            }
        }
        return null;
    }

    private static final class ListHolder {
        private static final String[] ID_METHODS = {
                "id", "userId", "doctorId", "pharmacistId", "appointmentId", "consultationId", "prescriptionId"
        };
    }
}
