package com.medisync.doctor.service;

import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.doctor.dto.DoctorProfileResponse;
import com.medisync.doctor.dto.DoctorProfileUpdateRequest;
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
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class DoctorProfileService {

    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public DoctorProfileService(CurrentUserService currentUserService,
                                DoctorProfileRepository doctorProfileRepository,
                                HospitalRepository hospitalRepository,
                                DepartmentRepository departmentRepository,
                                SpecializationRepository specializationRepository) {
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getProfile(Jwt jwt) {
        AppUser user = requireDoctor(jwt);
        return response(requireProfile(user.getId()));
    }

    @Transactional
    public DoctorProfileResponse updateProfile(Jwt jwt, DoctorProfileUpdateRequest request) {
        AppUser user = requireDoctor(jwt);
        DoctorProfile profile = doctorProfileRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        if (profile.getVerificationStatus() == VerificationStatus.VERIFIED) {
            ensureVerifiedIdentityUnchanged(profile, request);
            profile.updateBio(optional(request.bio()));
            return response(doctorProfileRepository.saveAndFlush(profile));
        }
        if (profile.isAwaitingReview()) {
            throw new ResourceConflictException("A submitted profile cannot be changed while it is awaiting review");
        }

        String registrationNumber = optional(request.medicalRegistrationNumber());
        String qualifications = optional(request.qualifications());
        if (registrationNumber != null
                && doctorProfileRepository.existsByMedicalRegistrationNumberIgnoreCaseAndIdNot(
                registrationNumber, profile.getId())) {
            throw new ResourceConflictException("This medical registration number is already in use");
        }

        validateReferences(request.hospitalId(), request.departmentId(), request.specializationId(), true);
        profile.updateProfessionalProfile(registrationNumber, request.hospitalId(), request.departmentId(),
                request.specializationId(), qualifications, request.yearsOfExperience(), optional(request.bio()));
        profile.updatePaymentDetails(optional(request.bankAccountHolder()), optional(request.bankName()),
                optional(request.bankBranch()), optional(request.bankAccountNumber()));
        return response(doctorProfileRepository.saveAndFlush(profile));
    }

    @Transactional
    public DoctorProfileResponse submitForVerification(Jwt jwt) {
        AppUser user = requireDoctor(jwt);
        DoctorProfile profile = doctorProfileRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (profile.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new ResourceConflictException("This doctor is already verified");
        }
        if (profile.isAwaitingReview()) {
            throw new ResourceConflictException("This profile is already awaiting verification");
        }
        if (!profile.isComplete()) {
            throw new InvalidRequestException("Complete all required professional fields before submitting");
        }
        validateReferences(profile.getHospitalId(), profile.getDepartmentId(), profile.getSpecializationId(), true);
        profile.submitForVerification();
        return response(doctorProfileRepository.saveAndFlush(profile));
    }

    private AppUser requireDoctor(Jwt jwt) {
        return currentUserService.requireRole(jwt, UserRole.DOCTOR,
                AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE);
    }

    private DoctorProfile requireProfile(UUID userId) {
        return doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
    }

    private void validateReferences(UUID hospitalId, UUID departmentId, UUID specializationId, boolean requireActive) {
        if (hospitalId == null && departmentId != null) {
            throw new InvalidRequestException("Select a hospital before selecting a department");
        }
        Hospital hospital = hospitalId == null ? null : hospitalRepository.findById(hospitalId)
                .filter(candidate -> !requireActive || candidate.isActive())
                .orElseThrow(() -> new InvalidRequestException("Select an active hospital"));
        Department department = departmentId == null ? null : departmentRepository.findById(departmentId)
                .filter(candidate -> !requireActive || candidate.isActive())
                .orElseThrow(() -> new InvalidRequestException("Select an active department"));
        if (hospital != null && department != null && !department.getHospitalId().equals(hospital.getId())) {
            throw new InvalidRequestException("The selected department does not belong to the selected hospital");
        }
        if (specializationId != null) {
            specializationRepository.findById(specializationId)
                    .filter(candidate -> !requireActive || candidate.isActive())
                    .orElseThrow(() -> new InvalidRequestException("Select an active specialization"));
        }
    }

    private void ensureVerifiedIdentityUnchanged(DoctorProfile profile, DoctorProfileUpdateRequest request) {
        boolean changed = suppliedStringChanged(request.medicalRegistrationNumber(), profile.getMedicalRegistrationNumber())
                || suppliedChanged(request.hospitalId(), profile.getHospitalId())
                || suppliedChanged(request.departmentId(), profile.getDepartmentId())
                || suppliedChanged(request.specializationId(), profile.getSpecializationId())
                || suppliedStringChanged(request.qualifications(), profile.getQualifications())
                || suppliedChanged(request.yearsOfExperience(), profile.getYearsOfExperience());
        if (changed) {
            throw new ResourceConflictException("Verified professional identity fields are locked");
        }
    }

    private boolean suppliedStringChanged(String supplied, String existing) {
        return supplied != null && !Objects.equals(optional(supplied), existing);
    }

    private boolean suppliedChanged(Object supplied, Object existing) {
        return supplied != null && !Objects.equals(supplied, existing);
    }

    private DoctorProfileResponse response(DoctorProfile profile) {
        Hospital hospital = profile.getHospitalId() == null ? null
                : hospitalRepository.findById(profile.getHospitalId()).orElse(null);
        Department department = profile.getDepartmentId() == null ? null
                : departmentRepository.findById(profile.getDepartmentId()).orElse(null);
        Specialization specialization = profile.getSpecializationId() == null ? null
                : specializationRepository.findById(profile.getSpecializationId()).orElse(null);
        return new DoctorProfileResponse(profile.getId(), profile.getMedicalRegistrationNumber(),
                profile.getHospitalId(), hospital == null ? null : hospital.getName(), profile.getDepartmentId(),
                department == null ? null : department.getName(), profile.getSpecializationId(),
                specialization == null ? null : specialization.getName(), profile.getQualifications(),
                profile.getYearsOfExperience(), profile.getBio(), profile.getVerificationStatus(),
                profile.getVerificationRejectionReason(), profile.getSubmittedForVerificationAt(),
                profile.getVerifiedAt(), profile.isComplete(), profile.isAwaitingReview(),
                profile.getVerificationStatus() != VerificationStatus.VERIFIED && !profile.isAwaitingReview(),
                profile.getBankAccountHolder(), profile.getBankName(), profile.getBankBranch(),
                profile.getBankAccountNumber());
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

