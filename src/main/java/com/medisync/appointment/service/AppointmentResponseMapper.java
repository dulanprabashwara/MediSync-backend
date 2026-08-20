package com.medisync.appointment.service;

import com.medisync.appointment.dto.AppointmentResponse;
import com.medisync.appointment.dto.AppointmentSymptomsResponse;
import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentSymptoms;
import com.medisync.appointment.repository.AppointmentSymptomsRepository;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import org.springframework.stereotype.Component;

@Component
public class AppointmentResponseMapper {

    private final AppointmentSymptomsRepository symptomsRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public AppointmentResponseMapper(AppointmentSymptomsRepository symptomsRepository,
                                     PatientProfileRepository patientProfileRepository,
                                     DoctorProfileRepository doctorProfileRepository,
                                     AppUserRepository appUserRepository,
                                     HospitalRepository hospitalRepository,
                                     DepartmentRepository departmentRepository,
                                     SpecializationRepository specializationRepository) {
        this.symptomsRepository = symptomsRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appUserRepository = appUserRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    public AppointmentResponse toResponse(Appointment appointment) {
        PatientProfile patient = patientProfileRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        DoctorProfile doctor = doctorProfileRepository.findById(appointment.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        AppUser patientUser = user(patient.getUserId());
        AppUser doctorUser = user(doctor.getUserId());
        Hospital hospital = hospitalRepository.findById(doctor.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
        Department department = departmentRepository.findById(doctor.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Specialization specialization = specializationRepository.findById(doctor.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
        AppointmentSymptoms symptoms = symptomsRepository.findByAppointmentId(appointment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment symptoms not found"));

        return new AppointmentResponse(
                appointment.getId(), appointment.getSlotId(), fullName(patientUser), "Dr. " + fullName(doctorUser),
                hospital.getName(), department.getName(), specialization.getName(), appointment.getScheduledStart(),
                appointment.getScheduledEnd(), appointment.getStatus(), AppointmentSymptomsResponse.from(symptoms),
                appointment.getDoctorRejectionReason(), appointment.getCancellationReason(), appointment.getCreatedAt(),
                appointment.getConfirmedAt(), appointment.getRejectedAt(), appointment.getCancelledAt());
    }

    private AppUser user(java.util.UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
    }

    private String fullName(AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
