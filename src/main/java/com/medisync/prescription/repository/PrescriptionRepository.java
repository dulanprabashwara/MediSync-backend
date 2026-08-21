package com.medisync.prescription.repository;

import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Optional<Prescription> findByConsultationIdAndStatus(UUID consultationId, PrescriptionStatus status);

    boolean existsByConsultationIdAndStatus(UUID consultationId, PrescriptionStatus status);

    List<Prescription> findByConsultationIdOrderByCreatedAtDesc(UUID consultationId);

    Page<Prescription> findByDoctorId(UUID doctorId, Pageable pageable);

    Page<Prescription> findByPatientIdAndStatusIn(UUID patientId, Collection<PrescriptionStatus> statuses,
                                                   Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select prescription from Prescription prescription where prescription.id = :id")
    Optional<Prescription> findByIdForUpdate(@Param("id") UUID id);
}
