package com.medisync.doctor.service;

import com.medisync.availability.dto.AppointmentSlotResponse;
import com.medisync.availability.repository.AppointmentSlotRepository;
import com.medisync.common.dto.PageResponse;
import com.medisync.config.AppointmentProperties;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.doctor.dto.DoctorDetailsResponse;
import com.medisync.doctor.dto.DoctorSummaryResponse;
import com.medisync.exception.InvalidRequestException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PatientDoctorDiscoveryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SLOT_RANGE_DAYS = 31;
    private static final ZoneId DEFAULT_PATIENT_ZONE = ZoneId.of("Asia/Colombo");

    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentProperties appointmentProperties;
    private final Clock clock;

    public PatientDoctorDiscoveryService(CurrentUserService currentUserService,
                                         DoctorProfileRepository doctorProfileRepository,
                                         AppUserRepository appUserRepository,
                                         HospitalRepository hospitalRepository,
                                         DepartmentRepository departmentRepository,
                                         SpecializationRepository specializationRepository,
                                         AppointmentSlotRepository slotRepository,
                                         AppointmentProperties appointmentProperties,
                                         Clock clock) {
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appUserRepository = appUserRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
        this.slotRepository = slotRepository;
        this.appointmentProperties = appointmentProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorSummaryResponse> search(Jwt jwt, String q, UUID hospitalId,
                                                      UUID departmentId, UUID specializationId,
                                                      int page, int size) {
        requirePatient(jwt);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 50");
        }
        String search = q == null || q.isBlank() ? null : q.trim();
        Page<DoctorProfile> doctors = doctorProfileRepository.searchDiscoverable(search, hospitalId, departmentId,
                specializationId, PageRequest.of(page, size));
        return PageResponse.from(doctors, this::toSummary);
    }

    @Transactional(readOnly = true)
    public DoctorDetailsResponse details(Jwt jwt, UUID doctorId) {
        requirePatient(jwt);
        return toDetails(requireDiscoverableDoctor(doctorId));
    }

    @Transactional(readOnly = true)
    public java.util.List<AppointmentSlotResponse> availableSlots(Jwt jwt, UUID doctorId,
                                                                  LocalDate from, LocalDate to) {
        requirePatient(jwt);
        requireDiscoverableDoctor(doctorId);
        if (from == null || to == null || to.isBefore(from)) {
            throw new InvalidRequestException("A valid from and to date range is required");
        }
        // The range is inclusive, so a 30-day difference contains 31 calendar days.
        if (ChronoUnit.DAYS.between(from, to) >= MAX_SLOT_RANGE_DAYS) {
            throw new InvalidRequestException("Available slots may be queried for at most 31 days");
        }

        OffsetDateTime startsAt = from.atStartOfDay(DEFAULT_PATIENT_ZONE).toOffsetDateTime();
        OffsetDateTime endsAt = to.plusDays(1).atStartOfDay(DEFAULT_PATIENT_ZONE).toOffsetDateTime();
        OffsetDateTime earliestBookable = OffsetDateTime.now(clock)
                .plusMinutes(appointmentProperties.minimumLeadMinutes());
        if (startsAt.isBefore(earliestBookable)) {
            startsAt = earliestBookable;
        }
        if (!startsAt.isBefore(endsAt)) {
            return java.util.List.of();
        }
        return slotRepository.findVisibleAvailableSlots(doctorId, startsAt, endsAt).stream()
                .map(AppointmentSlotResponse::from)
                .toList();
    }

    private void requirePatient(Jwt jwt) {
        currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE);
    }

    private DoctorProfile requireDiscoverableDoctor(UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        AppUser user = appUserRepository.findById(doctor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        if (user.getRole() != UserRole.DOCTOR || user.getStatus() != AccountStatus.ACTIVE
                || doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        Hospital hospital = hospitalRepository.findById(doctor.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        Department department = departmentRepository.findById(doctor.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        Specialization specialization = specializationRepository.findById(doctor.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        if (!hospital.isActive() || !department.isActive() || !specialization.isActive()) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        return doctor;
    }

    private DoctorSummaryResponse toSummary(DoctorProfile doctor) {
        AppUser user = requireUser(doctor);
        Hospital hospital = requireHospital(doctor);
        Department department = requireDepartment(doctor);
        Specialization specialization = requireSpecialization(doctor);
        String bio = doctor.getBio();
        String summary = bio == null || bio.length() <= 240 ? bio : bio.substring(0, 237) + "...";
        return new DoctorSummaryResponse(doctor.getId(), displayName(user), hospital.getId(), hospital.getName(),
                department.getId(), department.getName(), specialization.getId(), specialization.getName(),
                doctor.getQualifications(), doctor.getYearsOfExperience(), summary, true);
    }

    private DoctorDetailsResponse toDetails(DoctorProfile doctor) {
        AppUser user = requireUser(doctor);
        Hospital hospital = requireHospital(doctor);
        Department department = requireDepartment(doctor);
        Specialization specialization = requireSpecialization(doctor);
        return new DoctorDetailsResponse(doctor.getId(), displayName(user), hospital.getId(), hospital.getName(),
                department.getId(), department.getName(), specialization.getId(), specialization.getName(),
                doctor.getQualifications(), doctor.getYearsOfExperience(), doctor.getBio(), true);
    }

    private AppUser requireUser(DoctorProfile doctor) {
        return appUserRepository.findById(doctor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found"));
    }

    private Hospital requireHospital(DoctorProfile doctor) {
        return hospitalRepository.findById(doctor.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
    }

    private Department requireDepartment(DoctorProfile doctor) {
        return departmentRepository.findById(doctor.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private Specialization requireSpecialization(DoctorProfile doctor) {
        return specializationRepository.findById(doctor.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
    }

    private String displayName(AppUser user) {
        return "Dr. " + user.getFirstName() + " " + user.getLastName();
    }
}
