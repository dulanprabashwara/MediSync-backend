package com.medisync.consultation.repository;

import com.medisync.consultation.entity.ConsultationClinicalNote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationClinicalNoteRepository extends JpaRepository<ConsultationClinicalNote, UUID> {

    Optional<ConsultationClinicalNote> findByConsultationId(UUID consultationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select note from ConsultationClinicalNote note where note.consultationId = :consultationId")
    Optional<ConsultationClinicalNote> findByConsultationIdForUpdate(@Param("consultationId") UUID consultationId);
}
