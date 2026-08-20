package com.medisync.availability.service;

import com.medisync.availability.dto.AppointmentSlotResponse;
import com.medisync.availability.dto.AvailabilityWindowResponse;
import com.medisync.availability.dto.CreateAvailabilityRequest;
import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.DoctorAvailabilityWindow;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.availability.repository.DoctorAvailabilityWindowRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DoctorAvailabilityService {

    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(15, 20, 30, 45, 60);

    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorAvailabilityWindowRepository windowRepository;
    private final AppointmentSlotRepository slotRepository;

    public DoctorAvailabilityService(CurrentUserService currentUserService,
                                     DoctorProfileRepository doctorProfileRepository,
                                     DoctorAvailabilityWindowRepository windowRepository,
                                     AppointmentSlotRepository slotRepository) {
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.windowRepository = windowRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityWindowResponse> list(Jwt jwt) {
        DoctorProfile doctor = requireVerifiedDoctor(jwt, false);
        return windowRepository.findByDoctorIdOrderByStartsAtAsc(doctor.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AvailabilityWindowResponse create(Jwt jwt, CreateAvailabilityRequest request) {
        validateRequest(request);
        DoctorProfile doctor = requireVerifiedDoctor(jwt, true);

        if (windowRepository.existsActiveOverlap(doctor.getId(), request.startsAt(), request.endsAt())) {
            throw new ResourceConflictException("This availability period overlaps an existing active window");
        }

        DoctorAvailabilityWindow window = windowRepository.save(new DoctorAvailabilityWindow(
                doctor.getId(), request.startsAt(), request.endsAt(), request.slotDurationMinutes(),
                request.timeZone().trim()));

        List<AppointmentSlot> slots = generateSlots(window);
        if (slots.isEmpty()) {
            throw new InvalidRequestException("The availability period must contain at least one complete slot");
        }
        slotRepository.saveAll(slots);
        return AvailabilityWindowResponse.from(window, slots.stream().map(AppointmentSlotResponse::from).toList());
    }

    @Transactional
    public AvailabilityWindowResponse deactivate(Jwt jwt, UUID windowId) {
        DoctorProfile doctor = requireVerifiedDoctor(jwt, true);
        DoctorAvailabilityWindow window = windowRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability window not found"));
        requireOwnership(doctor.getId(), window.getDoctorId());
        if (!window.isActive()) {
            throw new ResourceConflictException("This availability window is already inactive");
        }
        if (!window.getEndsAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResourceConflictException("Past availability cannot be changed");
        }

        List<AppointmentSlot> slots = slotRepository.findByAvailabilityWindowIdForUpdate(windowId);
        if (slots.stream().anyMatch(slot -> slot.getStatus() == SlotStatus.RESERVED
                || slot.getStatus() == SlotStatus.BOOKED)) {
            throw new ResourceConflictException("Availability with reserved or booked appointments cannot be deactivated");
        }
        slots.stream().filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE).forEach(AppointmentSlot::block);
        window.deactivate();
        return AvailabilityWindowResponse.from(window, slots.stream().map(AppointmentSlotResponse::from).toList());
    }

    @Transactional
    public AppointmentSlotResponse block(Jwt jwt, UUID slotId) {
        DoctorProfile doctor = requireVerifiedDoctor(jwt, false);
        AppointmentSlot slot = lockedOwnedSlot(doctor.getId(), slotId);
        if (!slot.getStartsAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResourceConflictException("Past slots cannot be changed");
        }
        slot.block();
        return AppointmentSlotResponse.from(slot);
    }

    @Transactional
    public AppointmentSlotResponse unblock(Jwt jwt, UUID slotId) {
        DoctorProfile doctor = requireVerifiedDoctor(jwt, false);
        AppointmentSlot slot = lockedOwnedSlot(doctor.getId(), slotId);
        DoctorAvailabilityWindow window = windowRepository.findById(slot.getAvailabilityWindowId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability window not found"));
        if (!window.isActive() || !slot.getStartsAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResourceConflictException("This slot cannot be made available");
        }
        slot.unblock();
        return AppointmentSlotResponse.from(slot);
    }

    private DoctorProfile requireVerifiedDoctor(Jwt jwt, boolean forUpdate) {
        AppUser user = currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE);
        DoctorProfile doctor = (forUpdate
                ? doctorProfileRepository.findByUserIdForUpdate(user.getId())
                : doctorProfileRepository.findByUserId(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccessDeniedException("Only verified doctors can manage availability");
        }
        return doctor;
    }

    private void validateRequest(CreateAvailabilityRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new InvalidRequestException("Availability start must be before its end");
        }
        if (!request.startsAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new InvalidRequestException("Availability must start in the future");
        }
        if (!ALLOWED_DURATIONS.contains(request.slotDurationMinutes())) {
            throw new InvalidRequestException("Slot duration must be one of 15, 20, 30, 45, or 60 minutes");
        }
        try {
            ZoneId.of(request.timeZone().trim());
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("Time zone is invalid");
        }
    }

    private List<AppointmentSlot> generateSlots(DoctorAvailabilityWindow window) {
        List<AppointmentSlot> slots = new ArrayList<>();
        OffsetDateTime cursor = window.getStartsAt();
        OffsetDateTime slotEnd = cursor.plusMinutes(window.getSlotDurationMinutes());
        while (!slotEnd.isAfter(window.getEndsAt())) {
            slots.add(new AppointmentSlot(window.getId(), window.getDoctorId(), cursor, slotEnd));
            cursor = slotEnd;
            slotEnd = cursor.plusMinutes(window.getSlotDurationMinutes());
        }
        return slots;
    }

    private AppointmentSlot lockedOwnedSlot(UUID doctorId, UUID slotId) {
        AppointmentSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found"));
        requireOwnership(doctorId, slot.getDoctorId());
        return slot;
    }

    private void requireOwnership(UUID expectedDoctorId, UUID actualDoctorId) {
        if (!expectedDoctorId.equals(actualDoctorId)) {
            throw new AccessDeniedException("This availability belongs to another doctor");
        }
    }

    private AvailabilityWindowResponse toResponse(DoctorAvailabilityWindow window) {
        List<AppointmentSlotResponse> slots = slotRepository
                .findByAvailabilityWindowIdOrderByStartsAtAsc(window.getId()).stream()
                .map(AppointmentSlotResponse::from)
                .toList();
        return AvailabilityWindowResponse.from(window, slots);
    }
}
