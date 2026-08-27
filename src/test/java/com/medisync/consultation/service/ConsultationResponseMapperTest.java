package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.entity.AppointmentSymptoms;
import com.medisync.appointment.repository.AppointmentSymptomsRepository;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationResponseMapperTest {

    @Mock AppointmentSymptomsRepository symptomsRepository;
    @Mock HospitalRepository hospitalRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock SpecializationRepository specializationRepository;
    @Mock PrescriptionRepository prescriptionRepository;
    @Mock PrescriptionDispensationRepository dispensationRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private ConsultationResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ConsultationResponseMapper(
                symptomsRepository, hospitalRepository, departmentRepository, specializationRepository,
                prescriptionRepository, dispensationRepository, clock);
    }

    @Test
    void patientConsultationResponseContainsPatientCancellationReasonAndStatus() {
        String reason = "I am unavailable at this time.";
        ConsultationAccessService.ConsultationContext context = cancelledContext(true, reason);

        var response = mapper.toResponse(context);

        assertThat(response.status()).isEqualTo(com.medisync.consultation.entity.ConsultationStatus.CANCELLED);
        assertThat(response.appointmentStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_PATIENT);
        assertThat(response.cancellationReason()).isEqualTo(reason);
    }

    @Test
    void doctorConsultationResponseContainsDoctorCancellationReasonAndStatus() {
        String reason = "I am unavailable at the scheduled time.";
        ConsultationAccessService.ConsultationContext context = cancelledContext(false, reason);

        var response = mapper.toResponse(context);

        assertThat(response.status()).isEqualTo(com.medisync.consultation.entity.ConsultationStatus.CANCELLED);
        assertThat(response.appointmentStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_DOCTOR);
        assertThat(response.cancellationReason()).isEqualTo(reason);
    }

    private ConsultationAccessService.ConsultationContext cancelledContext(boolean cancelledByPatient,
                                                                            String reason) {
        AppUser patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        AppUser doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        PatientProfile patient = new PatientProfile(patientUser.getId());
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());

        Hospital hospital = new Hospital("Central Hospital", null, "Colombo", null, true);
        Department department = new Department(hospital.getId(), "General Medicine", true);
        Specialization specialization = new Specialization("General Medicine", null, true);
        doctor.updateProfessionalProfile("SLMC-123", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS", 8, null);
        doctor.verify(UUID.randomUUID());

        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(), start,
                start.plusMinutes(30));
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        if (cancelledByPatient) {
            appointment.cancelByPatient(reason);
        } else {
            appointment.cancelByDoctor(reason);
        }
        consultation.cancel();

        AppointmentSymptoms symptoms = new AppointmentSymptoms(
                appointment.getId(), 34, "Follow-up", "Headache", "Two days", null);
        when(hospitalRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));
        when(symptomsRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.of(symptoms));
        when(prescriptionRepository.findByConsultationIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        return new ConsultationAccessService.ConsultationContext(
                consultation, appointment, patient, doctor, patientUser, doctorUser);
    }
}
