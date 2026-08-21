package com.medisync.availability.service;

import com.medisync.availability.dto.CreateAvailabilityRequest;
import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.DoctorAvailabilityWindow;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.availability.repository.DoctorAvailabilityWindowRepository;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock DoctorProfileRepository doctorProfileRepository;
    @Mock DoctorAvailabilityWindowRepository windowRepository;
    @Mock AppointmentSlotRepository slotRepository;

    private DoctorAvailabilityService service;
    private Jwt jwt;
    private AppUser activeDoctorUser;
    private DoctorProfile verifiedDoctor;

    @BeforeEach
    void setUp() {
        service = new DoctorAvailabilityService(currentUserService, doctorProfileRepository, windowRepository,
                slotRepository, Clock.systemUTC());
        UUID authId = UUID.randomUUID();
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(authId.toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
        activeDoctorUser = new AppUser(authId, "doctor@example.com", "Asha", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        verifiedDoctor = new DoctorProfile(activeDoctorUser.getId());
        verifiedDoctor.verify(UUID.randomUUID());
    }

    @Test
    void verifiedActiveDoctorCreatesOnlyCompleteSlots() {
        allowVerifiedDoctor();
        when(windowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        OffsetDateTime start = future(2);
        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, start.plusMinutes(80), 30,
                "Asia/Colombo");

        var response = service.create(jwt, request);

        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots()).allMatch(slot -> slot.status() == SlotStatus.AVAILABLE);
        assertThat(response.slots().get(1).endsAt()).isEqualTo(start.plusMinutes(60));
        ArgumentCaptor<List<AppointmentSlot>> slots = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(slots.capture());
        assertThat(slots.getValue()).hasSize(2);
    }

    @Test
    void pendingDoctorCannotCreateAvailability() {
        DoctorProfile pending = new DoctorProfile(activeDoctorUser.getId());
        allowDoctorProfile(pending);
        assertThatThrownBy(() -> service.create(jwt, validRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedDoctorCannotCreateAvailability() {
        DoctorProfile rejected = new DoctorProfile(activeDoctorUser.getId());
        rejected.reject("Documents unclear");
        allowDoctorProfile(rejected);
        assertThatThrownBy(() -> service.create(jwt, validRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void suspendedDoctorCannotCreateAvailability() {
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE))
                .thenThrow(new AccessDeniedException("Suspended"));
        assertThatThrownBy(() -> service.create(jwt, validRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invalidOrPastWindowIsRejected() {
        OffsetDateTime start = future(2);
        assertThatThrownBy(() -> service.create(jwt,
                new CreateAvailabilityRequest(start, start, 30, "Asia/Colombo")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("before");
        assertThatThrownBy(() -> service.create(jwt,
                new CreateAvailabilityRequest(start.minusDays(2), start.minusDays(1), 30, "Asia/Colombo")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("future");
    }

    @Test
    void startExactlyAtServerNowIsRejected() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
        service = new DoctorAvailabilityService(currentUserService, doctorProfileRepository, windowRepository,
                slotRepository, Clock.fixed(now.toInstant(), ZoneId.of("UTC")));

        assertThatThrownBy(() -> service.create(jwt,
                new CreateAvailabilityRequest(now, now.plusHours(1), 30, "Asia/Colombo")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void doctorListQueriesOnlyUnexpiredWindowsAndFiltersElapsedSlots() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
        service = new DoctorAvailabilityService(currentUserService, doctorProfileRepository, windowRepository,
                slotRepository, Clock.fixed(now.toInstant(), ZoneId.of("UTC")));
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE)).thenReturn(activeDoctorUser);
        when(doctorProfileRepository.findByUserId(activeDoctorUser.getId())).thenReturn(Optional.of(verifiedDoctor));
        DoctorAvailabilityWindow partial = new DoctorAvailabilityWindow(verifiedDoctor.getId(),
                now.minusHours(1), now.plusHours(1), 30, "Asia/Colombo");
        AppointmentSlot elapsed = new AppointmentSlot(partial.getId(), verifiedDoctor.getId(),
                now.minusMinutes(30), now);
        AppointmentSlot futureSlot = new AppointmentSlot(partial.getId(), verifiedDoctor.getId(),
                now.plusMinutes(30), now.plusHours(1));
        when(windowRepository.findByDoctorIdAndEndsAtAfterOrderByStartsAtAsc(verifiedDoctor.getId(), now))
                .thenReturn(List.of(partial));
        when(slotRepository.findByAvailabilityWindowIdOrderByStartsAtAsc(partial.getId()))
                .thenReturn(List.of(elapsed, futureSlot));

        var result = service.list(jwt);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slots()).extracting(slot -> slot.id()).containsExactly(futureSlot.getId());
        verify(windowRepository).findByDoctorIdAndEndsAtAfterOrderByStartsAtAsc(verifiedDoctor.getId(), now);
    }

    @Test
    void invalidDurationAndTimeZoneAreRejected() {
        OffsetDateTime start = future(2);
        assertThatThrownBy(() -> service.create(jwt,
                new CreateAvailabilityRequest(start, start.plusHours(1), 25, "Asia/Colombo")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("duration");
        assertThatThrownBy(() -> service.create(jwt,
                new CreateAvailabilityRequest(start, start.plusHours(1), 30, "Mars/Olympus")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("zone");
    }

    @Test
    void overlappingWindowIsRejectedAfterDoctorLock() {
        allowVerifiedDoctor();
        CreateAvailabilityRequest request = validRequest();
        when(windowRepository.existsActiveOverlap(verifiedDoctor.getId(), request.startsAt(), request.endsAt()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(jwt, request))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("overlaps");
        verify(doctorProfileRepository).findByUserIdForUpdate(activeDoctorUser.getId());
    }

    @Test
    void doctorCannotDeactivateAnotherDoctorsWindow() {
        allowVerifiedDoctor();
        DoctorAvailabilityWindow other = new DoctorAvailabilityWindow(UUID.randomUUID(), future(2), future(3),
                30, "Asia/Colombo");
        when(windowRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.deactivate(jwt, other.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reservedWindowCannotBeDeactivated() {
        allowVerifiedDoctor();
        DoctorAvailabilityWindow window = new DoctorAvailabilityWindow(verifiedDoctor.getId(), future(2), future(3),
                30, "Asia/Colombo");
        AppointmentSlot reserved = new AppointmentSlot(window.getId(), verifiedDoctor.getId(), future(2),
                future(2).plusMinutes(30));
        reserved.reserve();
        when(windowRepository.findById(window.getId())).thenReturn(Optional.of(window));
        when(slotRepository.findByAvailabilityWindowIdForUpdate(window.getId())).thenReturn(List.of(reserved));

        assertThatThrownBy(() -> service.deactivate(jwt, window.getId()))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("reserved or booked");
    }

    @Test
    void reservedOrBookedSlotCannotBeBlocked() {
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE)).thenReturn(activeDoctorUser);
        when(doctorProfileRepository.findByUserId(activeDoctorUser.getId())).thenReturn(Optional.of(verifiedDoctor));
        AppointmentSlot slot = new AppointmentSlot(UUID.randomUUID(), verifiedDoctor.getId(), future(2),
                future(2).plusMinutes(30));
        slot.reserve();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        assertThatThrownBy(() -> service.block(jwt, slot.getId()))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("available");
        slot.book();
        assertThatThrownBy(() -> service.block(jwt, slot.getId()))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("available");
    }

    private void allowVerifiedDoctor() {
        allowDoctorProfile(verifiedDoctor);
    }

    private void allowDoctorProfile(DoctorProfile profile) {
        when(currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE)).thenReturn(activeDoctorUser);
        when(doctorProfileRepository.findByUserIdForUpdate(activeDoctorUser.getId())).thenReturn(Optional.of(profile));
    }

    private CreateAvailabilityRequest validRequest() {
        OffsetDateTime start = future(2);
        return new CreateAvailabilityRequest(start, start.plusHours(2), 30, "Asia/Colombo");
    }

    private OffsetDateTime future(long hours) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusHours(hours).withNano(0);
    }
}
