package com.medisync.consultation.repository;

import com.medisync.consultation.entity.ConsultationSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, UUID> {

    Optional<ConsultationSession> findByAppointmentId(UUID appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select consultation from ConsultationSession consultation where consultation.id = :id")
    Optional<ConsultationSession> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select consultation from ConsultationSession consultation where consultation.appointmentId = :appointmentId")
    Optional<ConsultationSession> findByAppointmentIdForUpdate(@Param("appointmentId") UUID appointmentId);
}
