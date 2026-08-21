package com.medisync.prescription.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionItemRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionResponseMapperTest {

    @Test
    void cancelledDoctorAndPatientResponsesHideRegimenAndDisableQr() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
        PrescriptionItemRepository itemRepository = mock(PrescriptionItemRepository.class);
        ConsultationSessionRepository consultationRepository = mock(ConsultationSessionRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        DoctorProfileRepository doctorRepository = mock(DoctorProfileRepository.class);
        PatientProfileRepository patientRepository = mock(PatientProfileRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        HospitalRepository hospitalRepository = mock(HospitalRepository.class);
        DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
        SpecializationRepository specializationRepository = mock(SpecializationRepository.class);

        AppUser patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        AppUser doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        PatientProfile patient = new PatientProfile(patientUser.getId());
        Hospital hospital = new Hospital("Central", null, "Colombo", null, true);
        Department department = new Department(hospital.getId(), "Medicine", true);
        Specialization specialization = new Specialization("Cardiology", null, true);
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());
        doctor.updateProfessionalProfile("SLMC-1", hospital.getId(), department.getId(), specialization.getId(),
                "MBBS", 10, null);
        doctor.verify(UUID.randomUUID());
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(),
                now.minusHours(2), now.minusHours(1));
        appointment.confirm();
        ConsultationSession consultation = new ConsultationSession(appointment.getId());
        consultation.start();
        consultation.complete();
        Prescription prescription = new Prescription(consultation.getId(), doctor.getId(), patient.getId());
        prescription.updateDraft(30, "Sensitive general instructions");
        prescription.issue(now.minusDays(1));
        prescription.cancel("Treatment changed", now);

        when(consultationRepository.findById(consultation.getId())).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(userRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        when(userRepository.findById(patientUser.getId())).thenReturn(Optional.of(patientUser));
        when(hospitalRepository.findById(hospital.getId())).thenReturn(Optional.of(hospital));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(specializationRepository.findById(specialization.getId())).thenReturn(Optional.of(specialization));
        PrescriptionResponseMapper mapper = new PrescriptionResponseMapper(itemRepository, consultationRepository,
                appointmentRepository, doctorRepository, patientRepository, userRepository, hospitalRepository,
                departmentRepository, specializationRepository,
                Clock.fixed(now.toInstant(), ZoneId.of("UTC")));

        var patientResponse = mapper.toPatientDetail(prescription);
        var doctorResponse = mapper.toDoctorResponse(prescription);

        assertThat(patientResponse.status()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(patientResponse.cancellationReason()).isEqualTo("Treatment changed");
        assertThat(patientResponse.items()).isEmpty();
        assertThat(patientResponse.generalInstructions()).isNull();
        assertThat(patientResponse.qrGenerationAllowed()).isFalse();
        assertThat(doctorResponse.items()).isEmpty();
        assertThat(doctorResponse.generalInstructions()).isNull();
        verify(itemRepository, never()).findByPrescriptionIdOrderByPositionAsc(prescription.getId());
    }
}
