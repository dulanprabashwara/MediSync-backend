package com.medisync.prescription.repository;

import com.medisync.prescription.entity.PrescriptionQrToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionQrTokenRepository extends JpaRepository<PrescriptionQrToken, UUID> {

    Optional<PrescriptionQrToken> findByPrescriptionId(UUID prescriptionId);

    boolean existsByTokenHash(String tokenHash);
}
