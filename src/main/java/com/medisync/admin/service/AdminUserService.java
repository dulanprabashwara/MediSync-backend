package com.medisync.admin.service;

import com.medisync.admin.dto.AdminBanHistoryResponse;
import com.medisync.admin.dto.AdminSelfProfileUpdateRequest;
import com.medisync.user.dto.UserResponse;
import com.medisync.admin.dto.AdminUserDetail;
import com.medisync.admin.dto.AdminUserSummary;
import com.medisync.audit.AuditActions;
import com.medisync.audit.service.AuditService;
import com.medisync.common.dto.PageResponse;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.media.MediaUrlService;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserAccountBan;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.repository.UserAccountBanRepository;
import com.medisync.user.service.CurrentUserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final UserAccountBanRepository banRepository;
    private final DoctorProfileRepository doctorRepository;
    private final PatientProfileRepository patientRepository;
    private final PharmacistProfileRepository pharmacistRepository;
    private final CurrentUserService currentUserService;
    private final MediaUrlService mediaUrlService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(AppUserRepository userRepository, UserAccountBanRepository banRepository,
                            DoctorProfileRepository doctorRepository, PatientProfileRepository patientRepository,
                            PharmacistProfileRepository pharmacistRepository, CurrentUserService currentUserService,
                            MediaUrlService mediaUrlService, AuditService auditService, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.banRepository = banRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.currentUserService = currentUserService;
        this.mediaUrlService = mediaUrlService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummary> list(int page, int size, String q, UserRole role, AccountStatus status,
                                               VerificationStatus verificationStatus,
                                               OffsetDateTime createdFrom, OffsetDateTime createdTo) {
        return list(page, size, q, role, status, verificationStatus, createdFrom, createdTo, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummary> listDoctors(int page, int size, String q, AccountStatus status,
                                                      VerificationStatus verificationStatus, UUID hospitalId,
                                                      UUID departmentId, UUID specializationId) {
        Collection<UUID> doctorUserIds = doctorRepository.findUserIdsByAdminFilters(
                verificationStatus, hospitalId, departmentId, specializationId);
        return list(page, size, q, UserRole.DOCTOR, status, null, null, null, doctorUserIds);
    }

    private PageResponse<AdminUserSummary> list(int page, int size, String q, UserRole role, AccountStatus status,
                                                VerificationStatus verificationStatus,
                                                OffsetDateTime createdFrom, OffsetDateTime createdTo,
                                                Collection<UUID> constrainedUserIds) {
        validatePage(page, size);
        if (verificationStatus != null && role != null
                && role != UserRole.DOCTOR && role != UserRole.PHARMACIST) {
            throw new InvalidRequestException(
                    "Professional verification status can only be used with a doctor or pharmacist role");
        }
        Collection<UUID> verifiedUserIds = null;
        if (verificationStatus != null) {
            if (role == UserRole.DOCTOR) {
                verifiedUserIds = doctorRepository.findUserIdsByVerificationStatus(verificationStatus);
            } else if (role == UserRole.PHARMACIST) {
                verifiedUserIds = pharmacistRepository.findUserIdsByVerificationStatus(verificationStatus);
            } else {
                java.util.LinkedHashSet<UUID> professionalIds = new java.util.LinkedHashSet<>(
                        doctorRepository.findUserIdsByVerificationStatus(verificationStatus));
                professionalIds.addAll(pharmacistRepository.findUserIdsByVerificationStatus(verificationStatus));
                verifiedUserIds = professionalIds;
            }
        }
        Collection<UUID> effectiveVerifiedUserIds = verifiedUserIds;
        Specification<AppUser> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("firstName")), pattern),
                        builder.like(builder.lower(root.get("lastName")), pattern),
                        builder.like(builder.lower(builder.concat(builder.concat(root.get("firstName"), " "),
                                root.get("lastName"))), pattern)
                ));
            }
            if (role != null) predicate = builder.and(predicate, builder.equal(root.get("role"), role));
            if (verificationStatus != null) {
                predicate = builder.and(predicate, root.get("id").in(effectiveVerifiedUserIds));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            if (constrainedUserIds != null) {
                predicate = builder.and(predicate, root.get("id").in(constrainedUserIds));
            }
            if (createdFrom != null) predicate = builder.and(predicate,
                    builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            if (createdTo != null) predicate = builder.and(predicate,
                    builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            return predicate;
        };
        Page<AppUser> result = userRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.from(result, this::summary);
    }

    @Transactional(readOnly = true)
    public AdminUserDetail details(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
                
        Map<String, Object> deletionMetadata = null;
        if (user.getStatus() == AccountStatus.DELETED) {
            deletionMetadata = new LinkedHashMap<>();
            put(deletionMetadata, "deletedAt", user.getDeletedAt());
            put(deletionMetadata, "deletedByUserId", user.getDeletedByUserId());
            put(deletionMetadata, "deletionReason", user.getDeletionReason());
            put(deletionMetadata, "deletionSource", user.getDeletionSource());
        }
        
        return new AdminUserDetail(summary(user), roleProfile(user), operationalCounts(user),
                banRepository.findByUserIdOrderByBannedAtDesc(userId).stream().map(this::banResponse).toList(),
                auditService.recentForUser(userId, 10), deletionMetadata);
    }

    @Transactional
    public AdminUserDetail ban(Jwt jwt, UUID userId, String rawReason) {
        AppUser admin = currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE);
        AppUser target = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        if (target.getRole() == UserRole.ADMIN) {
            throw new ResourceConflictException("Administrator accounts cannot be banned through this endpoint");
        }
        if (target.getStatus() == AccountStatus.BANNED
                || banRepository.findByUserIdAndUnbannedAtIsNull(userId).isPresent()) {
            throw new ResourceConflictException("This account is already banned");
        }
        String reason = rawReason == null ? "" : rawReason.trim();
        if (reason.length() < 3 || reason.length() > 1000) {
            throw new InvalidRequestException("Ban reason must be between 3 and 1000 characters");
        }
        AccountStatus previous = target.getStatus();
        target.ban();
        userRepository.saveAndFlush(target);
        banRepository.saveAndFlush(new UserAccountBan(userId, admin.getId(), reason, previous,
                OffsetDateTime.now(ZoneOffset.UTC)));
        auditService.record(admin, AuditActions.USER_BANNED, "APP_USER", userId,
                Map.of("previousStatus", previous.name(), "newStatus", AccountStatus.BANNED.name(),
                        "reasonProvided", true));
        return details(userId);
    }

    @Transactional
    public AdminUserDetail unban(Jwt jwt, UUID userId) {
        AppUser admin = currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE);
        AppUser target = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        UserAccountBan ban = banRepository.findByUserIdAndUnbannedAtIsNull(userId)
                .orElseThrow(() -> new ResourceConflictException("This account is not actively banned"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        target.restoreStatus(ban.getPreviousStatus());
        ban.close(admin.getId(), now);
        userRepository.saveAndFlush(target);
        banRepository.saveAndFlush(ban);
        auditService.record(admin, AuditActions.USER_UNBANNED, "APP_USER", userId,
                Map.of("previousStatus", AccountStatus.BANNED.name(), "newStatus", target.getStatus().name()));
        return details(userId);
    }

    private AdminUserSummary summary(AppUser user) {
        VerificationStatus verificationStatus = null;
        String registration = null;
        ProfessionalLabels labels = ProfessionalLabels.empty();
        if (user.getRole() == UserRole.DOCTOR) {
            DoctorProfile profile = doctorRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                verificationStatus = profile.getVerificationStatus();
                registration = profile.getMedicalRegistrationNumber();
                labels = doctorLabels(profile);
            }
        } else if (user.getRole() == UserRole.PHARMACIST) {
            PharmacistProfile profile = pharmacistRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                verificationStatus = profile.getVerificationStatus();
                registration = profile.getProfessionalRegistrationNumber();
                labels = new ProfessionalLabels(null, null, null, profile.getPharmacyName());
            }
        }
        OffsetDateTime lastActivity = jdbcTemplate.queryForObject(
                "SELECT GREATEST(?, COALESCE(MAX(occurred_at), ?)) FROM audit_events WHERE actor_user_id = ?",
                OffsetDateTime.class, user.getUpdatedAt(), user.getUpdatedAt(), user.getId());
        return new AdminUserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getRole(), user.getStatus(), verificationStatus, registration,
                labels.hospitalName(), labels.departmentName(), labels.specializationName(), labels.pharmacyName(),
                mediaUrlService.signedUrlOrNull(user.getProfileImageKey()), user.getCreatedAt(), user.getUpdatedAt(),
                lastActivity);
    }

    private Map<String, Object> roleProfile(AppUser user) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (user.getRole() == UserRole.PATIENT) {
            patientRepository.findByUserId(user.getId()).ifPresent(profile -> result.put("profileId", profile.getId()));
        } else if (user.getRole() == UserRole.DOCTOR) {
            doctorRepository.findByUserId(user.getId()).ifPresent(profile -> {
                put(result, "profileId", profile.getId());
                put(result, "medicalRegistrationNumber", profile.getMedicalRegistrationNumber());
                put(result, "verificationStatus", profile.getVerificationStatus());
                put(result, "hospitalId", profile.getHospitalId());
                put(result, "departmentId", profile.getDepartmentId());
                put(result, "specializationId", profile.getSpecializationId());
                ProfessionalLabels labels = doctorLabels(profile);
                put(result, "hospital", labels.hospitalName());
                put(result, "department", labels.departmentName());
                put(result, "specialization", labels.specializationName());
                put(result, "yearsOfExperience", profile.getYearsOfExperience());
                put(result, "bankAccountHolder", profile.getBankAccountHolder());
                put(result, "bankName", profile.getBankName());
                put(result, "bankBranch", profile.getBankBranch());
                put(result, "bankAccountNumber", profile.getBankAccountNumber());
            });
        } else if (user.getRole() == UserRole.PHARMACIST) {
            pharmacistRepository.findByUserId(user.getId()).ifPresent(profile -> {
                put(result, "profileId", profile.getId());
                put(result, "professionalRegistrationNumber", profile.getProfessionalRegistrationNumber());
                put(result, "pharmacyName", profile.getPharmacyName());
                put(result, "pharmacyRegistrationNumber", profile.getPharmacyRegistrationNumber());
                put(result, "verificationStatus", profile.getVerificationStatus());
            });
        }
        return result;
    }

    private Map<String, Long> operationalCounts(AppUser user) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("auditEvents", count("SELECT COUNT(*) FROM audit_events WHERE actor_user_id = ?", user.getId()));
        counts.put("messagesSent", count("SELECT COUNT(*) FROM consultation_messages WHERE sender_user_id = ?", user.getId()));
        if (user.getRole() == UserRole.PATIENT) {
            counts.put("appointments", count("SELECT COUNT(*) FROM appointments a JOIN patient_profiles p ON p.id = a.patient_id WHERE p.user_id = ?", user.getId()));
            counts.put("consultations", count("SELECT COUNT(*) FROM consultation_sessions c JOIN appointments a ON a.id = c.appointment_id JOIN patient_profiles p ON p.id = a.patient_id WHERE p.user_id = ?", user.getId()));
            counts.put("completedConsultations", count("SELECT COUNT(*) FROM consultation_sessions c JOIN appointments a ON a.id = c.appointment_id JOIN patient_profiles p ON p.id = a.patient_id WHERE p.user_id = ? AND c.status = 'COMPLETED'", user.getId()));
            counts.put("prescriptions", count("SELECT COUNT(*) FROM prescriptions r JOIN patient_profiles p ON p.id = r.patient_id WHERE p.user_id = ?", user.getId()));
            counts.put("dispensedPrescriptions", count("SELECT COUNT(*) FROM prescription_dispensations x JOIN prescriptions r ON r.id = x.prescription_id JOIN patient_profiles p ON p.id = r.patient_id WHERE p.user_id = ?", user.getId()));
        } else if (user.getRole() == UserRole.DOCTOR) {
            counts.put("appointments", count("SELECT COUNT(*) FROM appointments a JOIN doctor_profiles d ON d.id = a.doctor_id WHERE d.user_id = ?", user.getId()));
            counts.put("completedConsultations", count("SELECT COUNT(*) FROM consultation_sessions c JOIN appointments a ON a.id = c.appointment_id JOIN doctor_profiles d ON d.id = a.doctor_id WHERE d.user_id = ? AND c.status = 'COMPLETED'", user.getId()));
            counts.put("prescriptionsIssued", count("SELECT COUNT(*) FROM prescriptions r JOIN doctor_profiles d ON d.id = r.doctor_id WHERE d.user_id = ? AND r.issued_at IS NOT NULL", user.getId()));
        } else if (user.getRole() == UserRole.PHARMACIST) {
            counts.put("dispensations", count("SELECT COUNT(*) FROM prescription_dispensations x JOIN pharmacist_profiles p ON p.id = x.pharmacist_id WHERE p.user_id = ?", user.getId()));
        }
        return counts;
    }

    private long count(String sql, UUID userId) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return value == null ? 0 : value;
    }

    private AdminBanHistoryResponse banResponse(UserAccountBan ban) {
        return new AdminBanHistoryResponse(ban.getId(), ban.getReason(), ban.getPreviousStatus(), ban.getBannedBy(),
                ban.getBannedAt(), ban.getUnbannedBy(), ban.getUnbannedAt());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100");
        }
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private ProfessionalLabels doctorLabels(DoctorProfile profile) {
        return jdbcTemplate.query("""
                        SELECT hospital.name hospital_name, department.name department_name,
                               specialization.name specialization_name
                        FROM doctor_profiles doctor
                        LEFT JOIN hospitals hospital ON hospital.id = doctor.hospital_id
                        LEFT JOIN departments department ON department.id = doctor.department_id
                        LEFT JOIN specializations specialization ON specialization.id = doctor.specialization_id
                        WHERE doctor.id = ?
                        """,
                (row, index) -> new ProfessionalLabels(row.getString("hospital_name"),
                        row.getString("department_name"), row.getString("specialization_name"), null),
                profile.getId()).stream().findFirst().orElse(ProfessionalLabels.empty());
    }

    private record ProfessionalLabels(String hospitalName, String departmentName,
                                      String specializationName, String pharmacyName) {
        private static ProfessionalLabels empty() {
            return new ProfessionalLabels(null, null, null, null);
        }
    }


    @Transactional
    public UserResponse updateSelfProfile(Jwt jwt, AdminSelfProfileUpdateRequest request) {
        AppUser admin = currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE);

        admin.updateProfile(request.firstName().trim(), request.lastName().trim(), 
            request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        userRepository.saveAndFlush(admin);
        
        auditService.record(admin, AuditActions.USER_UPDATED, "APP_USER", admin.getId(), java.util.Map.of("updatedFields", "basicInfo"));
        return UserResponse.from(admin, mediaUrlService.signedUrlOrNull(admin.getProfileImageKey()));
    }
}
