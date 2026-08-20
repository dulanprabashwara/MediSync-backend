package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConsultationAccessService {

    private final CurrentUserService currentUserService;
    private final ConsultationSessionRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;

    public ConsultationAccessService(CurrentUserService currentUserService,
                                     ConsultationSessionRepository consultationRepository,
                                     AppointmentRepository appointmentRepository,
                                     PatientProfileRepository patientProfileRepository,
                                     DoctorProfileRepository doctorProfileRepository,
                                     AppUserRepository appUserRepository) {
        this.currentUserService = currentUserService;
        this.consultationRepository = consultationRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appUserRepository = appUserRepository;
    }

    public ConsultationContext requirePatient(Jwt jwt, UUID consultationId, boolean forUpdate) {
        AppUser currentUser = currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE);
        PatientProfile currentPatient = patientProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        ConsultationContext context = load(consultationId, forUpdate);
        if (!currentPatient.getId().equals(context.patient().getId())) {
            throw new AccessDeniedException("This consultation belongs to another patient");
        }
        return context;
    }

    public ConsultationContext requireDoctor(Jwt jwt, UUID consultationId, boolean forUpdate) {
        AppUser currentUser = currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE);
        DoctorProfile currentDoctor = doctorProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (currentDoctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccessDeniedException("Only verified doctors can access consultations");
        }
        ConsultationContext context = load(consultationId, forUpdate);
        if (!currentDoctor.getId().equals(context.doctor().getId())) {
            throw new AccessDeniedException("This consultation belongs to another doctor");
        }
        return context;
    }

    public void requireChatWritable(ConsultationContext context) {
        ConsultationStatus status = context.consultation().getStatus();
        if (!status.allowsMessages()) {
            throw new ResourceConflictException(
                    "This online consultation was cancelled and its message history is read-only");
        }
        if (context.appointment().getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResourceConflictException("Chat is unavailable because the appointment is not confirmed");
        }
        boolean participantsActive = context.patientUser().getRole() == UserRole.PATIENT
                && context.patientUser().getStatus() == AccountStatus.ACTIVE
                && context.doctorUser().getRole() == UserRole.DOCTOR
                && context.doctorUser().getStatus() == AccountStatus.ACTIVE
                && context.doctor().getVerificationStatus() == VerificationStatus.VERIFIED;
        if (!participantsActive) {
            throw new ResourceConflictException("Consultation chat is unavailable for the current care relationship");
        }
    }

    private ConsultationContext load(UUID consultationId, boolean forUpdate) {
        ConsultationSession consultation = (forUpdate
                ? consultationRepository.findByIdForUpdate(consultationId)
                : consultationRepository.findById(consultationId))
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));
        Appointment appointment = appointmentRepository.findById(consultation.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        PatientProfile patient = patientProfileRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        DoctorProfile doctor = doctorProfileRepository.findById(appointment.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        AppUser patientUser = appUserRepository.findById(patient.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found"));
        AppUser doctorUser = appUserRepository.findById(doctor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found"));
        return new ConsultationContext(consultation, appointment, patient, doctor, patientUser, doctorUser);
    }

    public record ConsultationContext(
            ConsultationSession consultation,
            Appointment appointment,
            PatientProfile patient,
            DoctorProfile doctor,
            AppUser patientUser,
            AppUser doctorUser
    ) {
    }
}
