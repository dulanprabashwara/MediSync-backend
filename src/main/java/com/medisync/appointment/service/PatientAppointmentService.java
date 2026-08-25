package com.medisync.appointment.service;

import com.medisync.appointment.dto.AppointmentResponse;
import com.medisync.appointment.dto.CancelAppointmentRequest;
import com.medisync.appointment.dto.CreateAppointmentRequest;
import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentSymptoms;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.appointment.repository.AppointmentSymptomsRepository;
import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.DoctorAvailabilityWindow;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.availability.repository.DoctorAvailabilityWindowRepository;
import com.medisync.common.dto.PageResponse;
import com.medisync.config.AppointmentProperties;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.consultation.service.ConsultationRealtimePublisher;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.repository.SpecializationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.medisync.notification.service.NotificationService;
import com.medisync.notification.NotificationType;
import com.medisync.consultation.service.VideoConsultationService;

@Service
public class PatientAppointmentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CurrentUserService currentUserService;
    private final PatientProfileRepository patientProfileRepository;
    private final AppointmentSlotRepository slotRepository;
    private final DoctorAvailabilityWindowRepository windowRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSymptomsRepository symptomsRepository;
    private final AppointmentResponseMapper responseMapper;
    private final AppointmentProperties properties;
    private final ConsultationSessionRepository consultationRepository;
    private final ConsultationRealtimePublisher realtimePublisher;
    private final Clock clock;
    private final NotificationService notificationService;
    private final VideoConsultationService videoConsultationService;

    public PatientAppointmentService(CurrentUserService currentUserService,
                                     PatientProfileRepository patientProfileRepository,
                                     AppointmentSlotRepository slotRepository,
                                     DoctorAvailabilityWindowRepository windowRepository,
                                     DoctorProfileRepository doctorProfileRepository,
                                     AppUserRepository appUserRepository,
                                     HospitalRepository hospitalRepository,
                                     DepartmentRepository departmentRepository,
                                     SpecializationRepository specializationRepository,
                                     AppointmentRepository appointmentRepository,
                                     AppointmentSymptomsRepository symptomsRepository,
                                     AppointmentResponseMapper responseMapper,
                                     AppointmentProperties properties,
                                     ConsultationSessionRepository consultationRepository,
                                     ConsultationRealtimePublisher realtimePublisher,
                                     Clock clock,
                                     NotificationService notificationService,
                                     VideoConsultationService videoConsultationService) {
        this.currentUserService = currentUserService;
        this.patientProfileRepository = patientProfileRepository;
        this.slotRepository = slotRepository;
        this.windowRepository = windowRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appUserRepository = appUserRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
        this.appointmentRepository = appointmentRepository;
        this.symptomsRepository = symptomsRepository;
        this.responseMapper = responseMapper;
        this.properties = properties;
        this.consultationRepository = consultationRepository;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
        this.notificationService = notificationService;
        this.videoConsultationService = videoConsultationService;
    }

    @Transactional
    public AppointmentResponse create(Jwt jwt, CreateAppointmentRequest request) {
        AppUser patientUser = currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE);

        // Locking the patient first serializes conflict checks even when two different slots are requested concurrently.
        PatientProfile patient = patientProfileRepository.findByUserIdForUpdate(patientUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        AppointmentSlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ResourceConflictException("This appointment slot is no longer available");
        }
        OffsetDateTime earliestAllowed = OffsetDateTime.now(clock)
                .plusMinutes(properties.minimumLeadMinutes());
        if (!slot.getStartsAt().isAfter(earliestAllowed)) {
            throw new ResourceConflictException("This appointment time is in the past or too close to book");
        }

        DoctorAvailabilityWindow window = windowRepository.findById(slot.getAvailabilityWindowId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability window not found"));
        if (!window.isActive() || !window.getDoctorId().equals(slot.getDoctorId())) {
            throw new ResourceConflictException("The doctor is no longer available at this time");
        }
        requireBookableDoctor(slot.getDoctorId());

        if (appointmentRepository.existsActivePatientOverlap(patient.getId(), slot.getStartsAt(), slot.getEndsAt())) {
            throw new ResourceConflictException("You already have an overlapping appointment request");
        }

        Appointment appointment = appointmentRepository.save(new Appointment(patient.getId(), slot.getDoctorId(),
                slot.getId(), slot.getStartsAt(), slot.getEndsAt()));
        symptomsRepository.save(new AppointmentSymptoms(appointment.getId(), request.reasonForVisit().trim(),
                request.symptoms().trim(), normalize(request.symptomDuration()), normalize(request.additionalNotes())));
        slot.reserve();

        // Notification: NEW_APPOINTMENT_REQUEST -> Doctor
        DoctorProfile doctorProfile = doctorProfileRepository.findById(slot.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        AppUser doctorUser = appUserRepository.findById(doctorProfile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found"));
                
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String formattedDateTime = slot.getStartsAt().format(formatter);
        String patientName = patientUser.getFirstName() + " " + patientUser.getLastName();

        notificationService.createNotification(
                doctorUser.getId(),
                patientUser.getId(),
                NotificationType.NEW_APPOINTMENT_REQUEST,
                "New consultation request",
                "You have a new consultation request from " + patientName + " for " + formattedDateTime + ".",
                "/doctor/appointments",
                "APPOINTMENT",
                appointment.getId(),
                "appointment:" + appointment.getId() + ":requested:" + doctorUser.getId()
        );

        return responseMapper.toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> list(Jwt jwt, int page, int size) {
        PatientProfile patient = currentPatient(jwt, false);
        validatePage(page, size);
        Page<Appointment> appointments = appointmentRepository.findByPatientId(patient.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scheduledStart")));
        return PageResponse.from(appointments, responseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse details(Jwt jwt, UUID appointmentId) {
        PatientProfile patient = currentPatient(jwt, false);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        requirePatientOwnership(patient.getId(), appointment);
        return responseMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Jwt jwt, UUID appointmentId, CancelAppointmentRequest request) {
        PatientProfile patient = currentPatient(jwt, false);
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        requirePatientOwnership(patient.getId(), appointment);
        if (!appointment.getScheduledStart().isAfter(OffsetDateTime.now(clock))) {
            throw new ResourceConflictException("Past appointments cannot be cancelled");
        }
        AppointmentStatus previousStatus = appointment.getStatus();
        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found"));
        if ((previousStatus == AppointmentStatus.REQUESTED && slot.getStatus() != SlotStatus.RESERVED)
                || (previousStatus == AppointmentStatus.CONFIRMED && slot.getStatus() != SlotStatus.BOOKED)) {
            throw new ResourceConflictException("Appointment and slot state are inconsistent");
        }
        ConsultationSession consultation = previousStatus == AppointmentStatus.CONFIRMED
                ? consultationRepository.findByAppointmentIdForUpdate(appointment.getId()).orElse(null)
                : null;
        if (consultation != null) {
            consultation.cancel();
            videoConsultationService.markSessionEnded(consultation.getId());
        }
        appointment.cancelByPatient(request == null ? null : normalize(request.reason()));
        slot.release();
        if (consultation != null) {
            realtimePublisher.publishAfterCommit(appointment,
                    ConsultationEvent.statusChanged(consultation.getId(), consultation.getStatus()));
        }

        // Notification: PATIENT_CANCELLED_APPOINTMENT -> Doctor
        DoctorProfile doctorProfile = doctorProfileRepository.findById(slot.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        AppUser doctorUser = appUserRepository.findById(doctorProfile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found"));
        AppUser patientUser = appUserRepository.findById(patient.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient user not found"));
                
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String formattedDateTime = appointment.getScheduledStart().format(formatter);
        String patientName = patientUser.getFirstName() + " " + patientUser.getLastName();

        notificationService.createNotification(
                doctorUser.getId(),
                patientUser.getId(),
                NotificationType.PATIENT_CANCELLED_APPOINTMENT,
                "Consultation cancelled",
                patientName + " cancelled the consultation scheduled for " + formattedDateTime + ".",
                "/doctor/appointments",
                "APPOINTMENT",
                appointment.getId(),
                "appointment:" + appointment.getId() + ":patient-cancelled:" + doctorUser.getId()
        );

        return responseMapper.toResponse(appointment);
    }

    private PatientProfile currentPatient(Jwt jwt, boolean forUpdate) {
        AppUser user = currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE);
        return (forUpdate ? patientProfileRepository.findByUserIdForUpdate(user.getId())
                : patientProfileRepository.findByUserId(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    private void requireBookableDoctor(UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceConflictException("The doctor is no longer available"));
        AppUser user = appUserRepository.findById(doctor.getUserId())
                .orElseThrow(() -> new ResourceConflictException("The doctor is no longer available"));
        boolean activeReferences = doctor.getHospitalId() != null && doctor.getDepartmentId() != null
                && doctor.getSpecializationId() != null
                && hospitalRepository.findById(doctor.getHospitalId()).map(value -> value.isActive()).orElse(false)
                && departmentRepository.findById(doctor.getDepartmentId()).map(value -> value.isActive()).orElse(false)
                && specializationRepository.findById(doctor.getSpecializationId()).map(value -> value.isActive()).orElse(false);
        if (user.getRole() != UserRole.DOCTOR || user.getStatus() != AccountStatus.ACTIVE
                || doctor.getVerificationStatus() != VerificationStatus.VERIFIED || !activeReferences) {
            throw new ResourceConflictException("The doctor is no longer available");
        }
    }

    private void requirePatientOwnership(UUID patientId, Appointment appointment) {
        if (!patientId.equals(appointment.getPatientId())) {
            throw new AccessDeniedException("This appointment belongs to another patient");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new com.medisync.exception.InvalidRequestException(
                    "Page must be non-negative and size must be between 1 and 50");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
