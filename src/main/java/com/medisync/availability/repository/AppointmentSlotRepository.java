package com.medisync.availability.repository;

import com.medisync.availability.entity.AppointmentSlot;
import com.medisync.availability.entity.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, UUID> {
    List<AppointmentSlot> findByAvailabilityWindowIdOrderByStartsAtAsc(UUID availabilityWindowId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from AppointmentSlot slot where slot.availabilityWindowId = :windowId order by slot.startsAt")
    List<AppointmentSlot> findByAvailabilityWindowIdForUpdate(@Param("windowId") UUID windowId);

    boolean existsByAvailabilityWindowIdAndStatusIn(UUID availabilityWindowId, List<SlotStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from AppointmentSlot slot where slot.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select slot from AppointmentSlot slot, DoctorAvailabilityWindow window
            where slot.availabilityWindowId = window.id
              and slot.doctorId = :doctorId
              and slot.status = com.medisync.availability.entity.SlotStatus.AVAILABLE
              and window.active = true
              and slot.startsAt > :startsAt
              and slot.startsAt < :endsAt
            order by slot.startsAt asc
            """)
    List<AppointmentSlot> findVisibleAvailableSlots(@Param("doctorId") UUID doctorId,
                                                    @Param("startsAt") OffsetDateTime startsAt,
                                                    @Param("endsAt") OffsetDateTime endsAt);
}
