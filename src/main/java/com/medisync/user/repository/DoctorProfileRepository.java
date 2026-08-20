package com.medisync.user.repository;

import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    Optional<DoctorProfile> findByUserId(UUID userId);
    List<DoctorProfile> findByVerificationStatusAndSubmittedForVerificationAtIsNotNullOrderBySubmittedForVerificationAtAsc(
            VerificationStatus verificationStatus);
    boolean existsByMedicalRegistrationNumberIgnoreCaseAndIdNot(String medicalRegistrationNumber, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from DoctorProfile profile where profile.userId = :userId")
    Optional<DoctorProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from DoctorProfile profile where profile.id = :id")
    Optional<DoctorProfile> findByIdForUpdate(@Param("id") UUID id);
}
