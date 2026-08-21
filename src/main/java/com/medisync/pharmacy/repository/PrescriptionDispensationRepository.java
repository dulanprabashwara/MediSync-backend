package com.medisync.pharmacy.repository;

import com.medisync.pharmacy.entity.PrescriptionDispensation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionDispensationRepository extends JpaRepository<PrescriptionDispensation, UUID> {
    Optional<PrescriptionDispensation> findByPrescriptionId(UUID prescriptionId);
    boolean existsByPrescriptionId(UUID prescriptionId);
    Page<PrescriptionDispensation> findByPharmacistIdOrderByDispensedAtDesc(UUID pharmacistId, Pageable pageable);
}
