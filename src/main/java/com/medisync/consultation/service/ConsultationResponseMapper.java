package com.medisync.consultation.service;

import com.medisync.appointment.dto.AppointmentSymptomsResponse;
import com.medisync.appointment.entity.AppointmentSymptoms;
import com.medisync.appointment.repository.AppointmentSymptomsRepository;
import com.medisync.consultation.dto.ConsultationPaymentSummary;
import com.medisync.consultation.dto.ConsultationResponse;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.pharmacy.dto.DispensingStatus;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class ConsultationResponseMapper {

    private final AppointmentSymptomsRepository symptomsRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDispensationRepository dispensationRepository;
    private final Clock clock;

    public ConsultationResponseMapper(AppointmentSymptomsRepository symptomsRepository,
                                      HospitalRepository hospitalRepository,
                                      DepartmentRepository departmentRepository,
                                      SpecializationRepository specializationRepository,
                                      PrescriptionRepository prescriptionRepository,
                                      PrescriptionDispensationRepository dispensationRepository,
                                      Clock clock) {
        this.symptomsRepository = symptomsRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.dispensationRepository = dispensationRepository;
        this.clock = clock;
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

        ConsultationPaymentSummary paymentSummary = buildPaymentSummary(context.consultation().getId());

        return new ConsultationResponse(
                context.consultation().getId(),
                context.appointment().getId(),
                context.consultation().getStatus(),
                context.appointment().getStatus(),
                context.appointment().getCancellationReason(),
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
                context.consultation().getUpdatedAt(),
                paymentSummary);
    }

    private ConsultationPaymentSummary buildPaymentSummary(UUID consultationId) {
        List<Prescription> prescriptions = prescriptionRepository
                .findByConsultationIdOrderByCreatedAtDesc(consultationId);
        Prescription issued = prescriptions.stream()
                .filter(p -> p.getStatus() == PrescriptionStatus.ISSUED)
                .findFirst()
                .orElse(null);
        if (issued == null) {
            return null;
        }
        boolean expired = issued.getValidUntil() != null
                && !OffsetDateTime.now(clock).isBefore(issued.getValidUntil());
        boolean dispensed = dispensationRepository.existsByPrescriptionId(issued.getId());
        DispensingStatus dispensingStatus = dispensed ? DispensingStatus.DISPENSED : DispensingStatus.NOT_DISPENSED;
        boolean qrAllowed = issued.isQrPaymentEligible() && !expired && !dispensed;
        return new ConsultationPaymentSummary(
                issued.getId(),
                issued.getStatus(),
                issued.getDoctorFeeAmount(),
                issued.getDoctorFeeCurrency(),
                issued.getDoctorFeeStatus(),
                issued.getDoctorFeeConfirmedAt(),
                qrAllowed,
                dispensingStatus,
                issued.getDoctorBankAccountHolder(),
                issued.getDoctorBankName(),
                issued.getDoctorBankBranch(),
                issued.getDoctorBankAccountNumber());
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
