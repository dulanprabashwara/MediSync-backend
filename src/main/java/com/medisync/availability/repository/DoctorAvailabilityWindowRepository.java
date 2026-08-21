package com.medisync.availability.repository;

import com.medisync.availability.entity.DoctorAvailabilityWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DoctorAvailabilityWindowRepository extends JpaRepository<DoctorAvailabilityWindow, UUID> {
    List<DoctorAvailabilityWindow> findByDoctorIdOrderByStartsAtAsc(UUID doctorId);

    List<DoctorAvailabilityWindow> findByDoctorIdAndEndsAtAfterOrderByStartsAtAsc(UUID doctorId,
                                                                                  OffsetDateTime now);

    @Query("""
            select count(window) > 0 from DoctorAvailabilityWindow window
            where window.doctorId = :doctorId
              and window.active = true
              and window.startsAt < :endsAt
              and window.endsAt > :startsAt
            """)
    boolean existsActiveOverlap(@Param("doctorId") UUID doctorId,
                                @Param("startsAt") OffsetDateTime startsAt,
                                @Param("endsAt") OffsetDateTime endsAt);
}
