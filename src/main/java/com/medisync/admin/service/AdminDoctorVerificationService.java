package com.medisync.admin.service;

import com.medisync.admin.dto.AdminDoctorReviewResponse;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminDoctorVerificationService {

    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public AdminDoctorVerificationService(CurrentUserService currentUserService,
                                          DoctorProfileRepository doctorProfileRepository,
                                          AppUserRepository appUserRepository,
                                          HospitalRepository hospitalRepository,
                                          DepartmentRepository departmentRepository,
                                          SpecializationRepository specializationRepository) {
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appUserRepository = appUserRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDoctorReviewResponse> pendingDoctors(Jwt jwt) {
        requireAdmin(jwt);
        return doctorProfileRepository
                .findByVerificationStatusAndSubmittedForVerificationAtIsNotNullOrderBySubmittedForVerificationAtAsc(
                        VerificationStatus.PENDING)
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDoctorReviewResponse doctor(Jwt jwt, UUID doctorId) {
        requireAdmin(jwt);
        return response(doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found")));
    }

    @Transactional
    public AdminDoctorReviewResponse verify(Jwt jwt, UUID doctorId) {
        AppUser admin = requireAdmin(jwt);
        DoctorProfile profile = lockPendingSubmission(doctorId);
        validateCompleteActiveProfile(profile);
        AppUser doctor = appUserRepository.findByIdForUpdate(profile.getUserId())
                .filter(user -> user.getRole() == UserRole.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found"));

        profile.verify(admin.getId());
        doctor.activate();
        doctorProfileRepository.saveAndFlush(profile);
        appUserRepository.saveAndFlush(doctor);
        return response(profile);
    }

    @Transactional
    public AdminDoctorReviewResponse reject(Jwt jwt, UUID doctorId, String reason) {
        requireAdmin(jwt);
        if (reason == null || reason.isBlank()) {
            throw new InvalidRequestException("A rejection reason is required");
        }
        DoctorProfile profile = lockPendingSubmission(doctorId);
        profile.reject(reason.trim());
        return response(doctorProfileRepository.saveAndFlush(profile));
    }

    private AppUser requireAdmin(Jwt jwt) {
        return currentUserService.requireRole(jwt, UserRole.ADMIN, AccountStatus.ACTIVE);
    }

    private DoctorProfile lockPendingSubmission(UUID doctorId) {
        DoctorProfile profile = doctorProfileRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (!profile.isAwaitingReview()) {
            throw new ResourceConflictException("This verification submission has already been processed");
        }
        return profile;
    }

    private void validateCompleteActiveProfile(DoctorProfile profile) {
        if (!profile.isComplete()) {
            throw new InvalidRequestException("The doctor profile is incomplete");
        }
        Hospital hospital = hospitalRepository.findById(profile.getHospitalId())
                .filter(Hospital::isActive)
                .orElseThrow(() -> new InvalidRequestException("The selected hospital is no longer active"));
        Department department = departmentRepository.findById(profile.getDepartmentId())
                .filter(Department::isActive)
                .orElseThrow(() -> new InvalidRequestException("The selected department is no longer active"));
        if (!department.getHospitalId().equals(hospital.getId())) {
            throw new InvalidRequestException("The doctor's department does not belong to the selected hospital");
        }
        specializationRepository.findById(profile.getSpecializationId())
                .filter(Specialization::isActive)
                .orElseThrow(() -> new InvalidRequestException("The selected specialization is no longer active"));
    }

    private AdminDoctorReviewResponse response(DoctorProfile profile) {
        AppUser user = appUserRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found"));
        Hospital hospital = profile.getHospitalId() == null ? null
                : hospitalRepository.findById(profile.getHospitalId()).orElse(null);
        Department department = profile.getDepartmentId() == null ? null
                : departmentRepository.findById(profile.getDepartmentId()).orElse(null);
        Specialization specialization = profile.getSpecializationId() == null ? null
                : specializationRepository.findById(profile.getSpecializationId()).orElse(null);
        return new AdminDoctorReviewResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), profile.getMedicalRegistrationNumber(), profile.getHospitalId(),
                hospital == null ? null : hospital.getName(), profile.getDepartmentId(),
                department == null ? null : department.getName(), profile.getSpecializationId(),
                specialization == null ? null : specialization.getName(), profile.getQualifications(),
                profile.getYearsOfExperience(), profile.getBio(), profile.getVerificationStatus(),
                profile.getVerificationRejectionReason(), profile.getSubmittedForVerificationAt(),
                profile.getVerifiedAt());
    }
}
