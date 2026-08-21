package com.medisync.user.repository;

import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PharmacistProfileRepository extends JpaRepository<PharmacistProfile, UUID> {
    Optional<PharmacistProfile> findByUserId(UUID userId);
    List<PharmacistProfile> findByVerificationStatusAndSubmittedForVerificationAtIsNotNullOrderBySubmittedForVerificationAtAsc(
            VerificationStatus verificationStatus);
    boolean existsByProfessionalRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from PharmacistProfile profile where profile.userId = :userId")
    Optional<PharmacistProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from PharmacistProfile profile where profile.id = :id")
    Optional<PharmacistProfile> findByIdForUpdate(@Param("id") UUID id);
}
