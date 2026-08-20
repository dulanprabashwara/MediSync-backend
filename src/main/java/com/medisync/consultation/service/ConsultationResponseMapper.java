package com.medisync.consultation.service;

import com.medisync.appointment.dto.AppointmentSymptomsResponse;
import com.medisync.appointment.entity.AppointmentSymptoms;
import com.medisync.appointment.repository.AppointmentSymptomsRepository;
import com.medisync.consultation.dto.ConsultationResponse;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import org.springframework.stereotype.Component;

@Component
public class ConsultationResponseMapper {

    private final AppointmentSymptomsRepository symptomsRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public ConsultationResponseMapper(AppointmentSymptomsRepository symptomsRepository,
                                      HospitalRepository hospitalRepository,
                                      DepartmentRepository departmentRepository,
                                      SpecializationRepository specializationRepository) {
        this.symptomsRepository = symptomsRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    public ConsultationResponse toResponse(ConsultationAccessService.ConsultationContext context) {
        Hospital hospital = hospitalRepository.findById(context.doctor().getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
        Department department = departmentRepository.findById(context.doctor().getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Specialization specialization = specializationRepository.findById(context.doctor().getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
        AppointmentSymptoms symptoms = symptomsRepository.findByAppointmentId(context.appointment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment symptoms not found"));

        return new ConsultationResponse(
                context.consultation().getId(),
                context.appointment().getId(),
                context.consultation().getStatus(),
                fullName(context.patientUser()),
                "Dr. " + fullName(context.doctorUser()),
                hospital.getName(),
                department.getName(),
                specialization.getName(),
                context.appointment().getScheduledStart(),
                context.appointment().getScheduledEnd(),
                AppointmentSymptomsResponse.from(symptoms),
                chatEnabled(context),
                context.consultation().getStartedAt(),
                context.consultation().getCompletedAt(),
                context.consultation().getCancelledAt(),
                context.consultation().getCreatedAt(),
                context.consultation().getUpdatedAt());
    }

    private String fullName(com.medisync.user.entity.AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private boolean chatEnabled(ConsultationAccessService.ConsultationContext context) {
        return context.consultation().getStatus().allowsMessages()
                && context.appointment().getStatus() == AppointmentStatus.CONFIRMED
                && context.patientUser().getRole() == UserRole.PATIENT
                && context.patientUser().getStatus() == AccountStatus.ACTIVE
                && context.doctorUser().getRole() == UserRole.DOCTOR
                && context.doctorUser().getStatus() == AccountStatus.ACTIVE
                && context.doctor().getVerificationStatus() == VerificationStatus.VERIFIED;
    }
}
