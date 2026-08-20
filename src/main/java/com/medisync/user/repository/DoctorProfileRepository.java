package com.medisync.user.repository;

import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query(value = """
            SELECT profile.*
            FROM doctor_profiles profile
            JOIN app_users app_user ON app_user.id = profile.user_id
            JOIN hospitals hospital ON hospital.id = profile.hospital_id
            JOIN departments department ON department.id = profile.department_id
            JOIN specializations specialization ON specialization.id = profile.specialization_id
            WHERE app_user.role = 'DOCTOR'
              AND app_user.status = 'ACTIVE'
              AND profile.verification_status = 'VERIFIED'
              AND hospital.active = TRUE
              AND department.active = TRUE
              AND specialization.active = TRUE
              AND (:q IS NULL OR LOWER(CONCAT(app_user.first_name, ' ', app_user.last_name)) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:hospitalId IS NULL OR profile.hospital_id = :hospitalId)
              AND (:departmentId IS NULL OR profile.department_id = :departmentId)
              AND (:specializationId IS NULL OR profile.specialization_id = :specializationId)
            ORDER BY app_user.first_name, app_user.last_name
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM doctor_profiles profile
            JOIN app_users app_user ON app_user.id = profile.user_id
            JOIN hospitals hospital ON hospital.id = profile.hospital_id
            JOIN departments department ON department.id = profile.department_id
            JOIN specializations specialization ON specialization.id = profile.specialization_id
            WHERE app_user.role = 'DOCTOR'
              AND app_user.status = 'ACTIVE'
              AND profile.verification_status = 'VERIFIED'
              AND hospital.active = TRUE
              AND department.active = TRUE
              AND specialization.active = TRUE
              AND (:q IS NULL OR LOWER(CONCAT(app_user.first_name, ' ', app_user.last_name)) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:hospitalId IS NULL OR profile.hospital_id = :hospitalId)
              AND (:departmentId IS NULL OR profile.department_id = :departmentId)
              AND (:specializationId IS NULL OR profile.specialization_id = :specializationId)
            """, nativeQuery = true)
    Page<DoctorProfile> searchDiscoverable(@Param("q") String q,
                                           @Param("hospitalId") UUID hospitalId,
                                           @Param("departmentId") UUID departmentId,
                                           @Param("specializationId") UUID specializationId,
                                           Pageable pageable);
}
