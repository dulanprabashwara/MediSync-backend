package com.medisync.appointment.service;

import com.medisync.appointment.dto.AppointmentResponse;
import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.consultation.service.ConsultationRealtimePublisher;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class DoctorAppointmentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentResponseMapper responseMapper;
    private final ConsultationSessionRepository consultationRepository;
    private final ConsultationRealtimePublisher realtimePublisher;

    public DoctorAppointmentService(CurrentUserService currentUserService,
                                    DoctorProfileRepository doctorProfileRepository,
                                    AppointmentRepository appointmentRepository,
                                    AppointmentSlotRepository slotRepository,
                                    AppointmentResponseMapper responseMapper,
                                    ConsultationSessionRepository consultationRepository,
                                    ConsultationRealtimePublisher realtimePublisher) {
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.responseMapper = responseMapper;
        this.consultationRepository = consultationRepository;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> list(Jwt jwt, AppointmentStatus status, int page, int size) {
        DoctorProfile doctor = currentDoctor(jwt);
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scheduledStart"));
        Page<Appointment> appointments = status == null
                ? appointmentRepository.findByDoctorId(doctor.getId(), pageable)
                : appointmentRepository.findByDoctorIdAndStatus(doctor.getId(), status, pageable);
        return PageResponse.from(appointments, responseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse details(Jwt jwt, UUID appointmentId) {
        DoctorProfile doctor = currentDoctor(jwt);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        requireDoctorOwnership(doctor.getId(), appointment);
        return responseMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse accept(Jwt jwt, UUID appointmentId) {
        DoctorProfile doctor = currentDoctor(jwt);
        Appointment appointment = lockedOwnedAppointment(doctor.getId(), appointmentId);
        requireFuture(appointment);
        AppointmentSlot slot = lockedSlot(appointment);
        if (slot.getStatus() != SlotStatus.RESERVED) {
            throw new ResourceConflictException("The appointment slot is not reserved");
        }
        appointment.confirm();
        slot.book();
        ConsultationSession consultation = consultationRepository.findByAppointmentId(appointment.getId())
                .orElseGet(() -> consultationRepository.save(new ConsultationSession(appointment.getId())));
        if (consultation.getStatus() != ConsultationStatus.SCHEDULED) {
            throw new ResourceConflictException("The consultation session is in an invalid state");
        }
        realtimePublisher.publishAfterCommit(appointment,
                ConsultationEvent.statusChanged(consultation.getId(), consultation.getStatus()));
        return responseMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse reject(Jwt jwt, UUID appointmentId, String reason) {
        DoctorProfile doctor = currentDoctor(jwt);
        Appointment appointment = lockedOwnedAppointment(doctor.getId(), appointmentId);
        AppointmentSlot slot = lockedSlot(appointment);
        if (slot.getStatus() != SlotStatus.RESERVED) {
            throw new ResourceConflictException("The appointment slot is not reserved");
        }
        appointment.reject(reason.trim());
        slot.release();
        return responseMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Jwt jwt, UUID appointmentId, String reason) {
        DoctorProfile doctor = currentDoctor(jwt);
        Appointment appointment = lockedOwnedAppointment(doctor.getId(), appointmentId);
        requireFuture(appointment);
        AppointmentSlot slot = lockedSlot(appointment);
        if (slot.getStatus() != SlotStatus.BOOKED) {
            throw new ResourceConflictException("The appointment slot is not booked");
        }
        ConsultationSession consultation = consultationRepository.findByAppointmentIdForUpdate(appointment.getId())
                .orElse(null);
        if (consultation != null) {
            consultation.cancel();
        }
        appointment.cancelByDoctor(reason.trim());
        slot.release();
        if (consultation != null) {
            realtimePublisher.publishAfterCommit(appointment,
                    ConsultationEvent.statusChanged(consultation.getId(), consultation.getStatus()));
        }
        return responseMapper.toResponse(appointment);
    }

    private DoctorProfile currentDoctor(Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE);
        DoctorProfile doctor = doctorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccessDeniedException("Only verified doctors can manage appointments");
        }
        return doctor;
    }

    private Appointment lockedOwnedAppointment(UUID doctorId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        requireDoctorOwnership(doctorId, appointment);
        return appointment;
    }

    private AppointmentSlot lockedSlot(Appointment appointment) {
        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found"));
        if (!appointment.getDoctorId().equals(slot.getDoctorId())) {
            throw new ResourceConflictException("Appointment and slot state are inconsistent");
        }
        return slot;
    }

    private void requireDoctorOwnership(UUID doctorId, Appointment appointment) {
        if (!doctorId.equals(appointment.getDoctorId())) {
            throw new AccessDeniedException("This appointment belongs to another doctor");
        }
    }

    private void requireFuture(Appointment appointment) {
        if (!appointment.getScheduledStart().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResourceConflictException("Past appointments cannot be processed");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 50");
        }
    }
}
