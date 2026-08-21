package com.medisync.prescription.repository;

import com.medisync.prescription.entity.PrescriptionQrToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionQrTokenRepository extends JpaRepository<PrescriptionQrToken, UUID> {

    Optional<PrescriptionQrToken> findByPrescriptionId(UUID prescriptionId);

    Optional<PrescriptionQrToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PrescriptionQrToken token where token.tokenHash = :tokenHash")
    Optional<PrescriptionQrToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PrescriptionQrToken token where token.prescriptionId = :prescriptionId")
    Optional<PrescriptionQrToken> findByPrescriptionIdForUpdate(@Param("prescriptionId") UUID prescriptionId);

    boolean existsByTokenHash(String tokenHash);
}
