package com.medisync.prescription.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.consultation.entity.ConsultationSession;
import com.medisync.consultation.repository.ConsultationSessionRepository;
import com.medisync.department.entity.Department;
import com.medisync.department.repository.DepartmentRepository;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.pharmacy.dto.DispensingStatus;
import com.medisync.pharmacy.entity.PrescriptionDispensation;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.dto.DoctorPrescriptionResponse;
import com.medisync.prescription.dto.PatientPrescriptionDetail;
import com.medisync.prescription.dto.PatientPrescriptionSummary;
import com.medisync.prescription.dto.PrescriptionItemResponse;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionItem;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionItemRepository;
import com.medisync.specialization.entity.Specialization;
import com.medisync.specialization.repository.SpecializationRepository;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PrescriptionResponseMapper {

    private final PrescriptionItemRepository itemRepository;
    private final ConsultationSessionRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AppUserRepository appUserRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;
    private final PrescriptionDispensationRepository dispensationRepository;
    private final Clock clock;

    public PrescriptionResponseMapper(PrescriptionItemRepository itemRepository,
                                      ConsultationSessionRepository consultationRepository,
                                      AppointmentRepository appointmentRepository,
                                      DoctorProfileRepository doctorProfileRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      AppUserRepository appUserRepository,
                                      HospitalRepository hospitalRepository,
                                      DepartmentRepository departmentRepository,
                                      SpecializationRepository specializationRepository,
                                      PrescriptionDispensationRepository dispensationRepository,
                                      Clock clock) {
        this.itemRepository = itemRepository;
        this.consultationRepository = consultationRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.appUserRepository = appUserRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
        this.dispensationRepository = dispensationRepository;
        this.clock = clock;
    }

    public DoctorPrescriptionResponse toDoctorResponse(Prescription prescription) {
        RelatedData data = relatedData(prescription);
        List<PrescriptionItemResponse> items = visibleItems(prescription);
        DispensingData dispensing = dispensingData(prescription);
        CancellationEligibility cancellation = cancellationEligibility(prescription, data, dispensing);
        return new DoctorPrescriptionResponse(prescription.getId(), prescription.getConsultationId(),
                data.consultation().getStatus(), prescription.getStatus(), fullName(data.patientUser()),
                "Dr. " + fullName(data.doctorUser()),
                data.doctor().getMedicalRegistrationNumber(), data.hospital().getName(), data.department().getName(),
                data.specialization().getName(), data.appointment().getScheduledStart(), prescription.getValidityDays(),
                visibleGeneralInstructions(prescription), prescription.getDoctorFeeAmount(),
                prescription.getDoctorFeeCurrency(), prescription.getDoctorFeeStatus(),
                prescription.getDoctorFeeConfirmedAt(), items, prescription.getIssuedAt(), prescription.getValidUntil(),
                expired(prescription), dispensing.status(), dispensing.dispensedAt(), dispensing.pharmacyName(),
                prescription.getCancelledAt(), prescription.getCancellationReason(),
                prescription.getCreatedAt(), prescription.getUpdatedAt(),
                cancellation.allowed(), cancellation.reason());
    }

    public PatientPrescriptionSummary toPatientSummary(Prescription prescription) {
        RelatedData data = relatedData(prescription);
        DispensingData dispensing = dispensingData(prescription);
        return new PatientPrescriptionSummary(prescription.getId(), prescription.getConsultationId(),
                "Dr. " + fullName(data.doctorUser()), data.specialization().getName(), data.hospital().getName(),
                prescription.getIssuedAt(), prescription.getValidUntil(), prescription.getStatus(),
                expired(prescription), dispensing.status(), prescription.getDoctorFeeAmount(),
                prescription.getDoctorFeeCurrency(), prescription.getDoctorFeeStatus(),
                dispensing.dispensedAt(), dispensing.pharmacyName(),
                prescription.getStatus() == PrescriptionStatus.CANCELLED
                        ? 0 : itemRepository.countByPrescriptionId(prescription.getId()));
    }

    public PatientPrescriptionDetail toPatientDetail(Prescription prescription) {
        RelatedData data = relatedData(prescription);
        boolean expired = expired(prescription);
        DispensingData dispensing = dispensingData(prescription);
        boolean generationAllowed = prescription.getStatus() == PrescriptionStatus.ISSUED && !expired
                && prescription.isQrPaymentEligible()
                && dispensing.status() == DispensingStatus.NOT_DISPENSED;
        return new PatientPrescriptionDetail(prescription.getId(), prescription.getConsultationId(),
                fullName(data.patientUser()), "Dr. " + fullName(data.doctorUser()),
                data.doctor().getMedicalRegistrationNumber(), data.hospital().getName(), data.department().getName(),
                data.specialization().getName(), data.appointment().getScheduledStart(), prescription.getIssuedAt(),
                prescription.getValidUntil(), prescription.getStatus(), expired,
                dispensing.status(), prescription.getDoctorFeeAmount(), prescription.getDoctorFeeCurrency(),
                prescription.getDoctorFeeStatus(), prescription.getDoctorFeeConfirmedAt(),
                dispensing.dispensedAt(), dispensing.pharmacyName(),
                visibleGeneralInstructions(prescription), visibleItems(prescription),
                prescription.getCancellationReason(), prescription.getCancelledAt(), generationAllowed);
    }

    private List<PrescriptionItemResponse> visibleItems(Prescription prescription) {
        return prescription.getStatus() == PrescriptionStatus.CANCELLED ? List.of() : items(prescription);
    }

    private String visibleGeneralInstructions(Prescription prescription) {
        return prescription.getStatus() == PrescriptionStatus.CANCELLED
                ? null : prescription.getGeneralInstructions();
    }

    private List<PrescriptionItemResponse> items(Prescription prescription) {
        return itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId()).stream()
                .map(PrescriptionItemResponse::from)
                .toList();
    }

    private boolean expired(Prescription prescription) {
        return prescription.getValidUntil() != null && !OffsetDateTime.now(clock).isBefore(prescription.getValidUntil());
    }

    private DispensingData dispensingData(Prescription prescription) {
        PrescriptionDispensation dispensation = dispensationRepository.findByPrescriptionId(prescription.getId())
                .orElse(null);
        return dispensation == null
                ? new DispensingData(DispensingStatus.NOT_DISPENSED, null, null)
                : new DispensingData(DispensingStatus.DISPENSED, dispensation.getDispensedAt(),
                dispensation.getPharmacyNameSnapshot());
    }

    private RelatedData relatedData(Prescription prescription) {
        ConsultationSession consultation = consultationRepository.findById(prescription.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));
        Appointment appointment = appointmentRepository.findById(consultation.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        DoctorProfile doctor = doctorProfileRepository.findById(prescription.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        PatientProfile patient = patientProfileRepository.findById(prescription.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        AppUser doctorUser = appUserRepository.findById(doctor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found"));
        AppUser patientUser = appUserRepository.findById(patient.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found"));
        Hospital hospital = hospitalRepository.findById(doctor.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
        Department department = departmentRepository.findById(doctor.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Specialization specialization = specializationRepository.findById(doctor.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
        if (!appointment.getDoctorId().equals(doctor.getId()) || !appointment.getPatientId().equals(patient.getId())) {
            throw new com.medisync.exception.ResourceConflictException(
                    "Prescription and consultation ownership are inconsistent");
        }
        return new RelatedData(consultation, appointment, doctor, patient, doctorUser, patientUser, hospital, department,
                specialization);
    }

    private String fullName(AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private record RelatedData(ConsultationSession consultation, Appointment appointment,
                               DoctorProfile doctor, PatientProfile patient,
                               AppUser doctorUser, AppUser patientUser, Hospital hospital, Department department,
                               Specialization specialization) {}

    private record DispensingData(DispensingStatus status, OffsetDateTime dispensedAt, String pharmacyName) {}

    private record CancellationEligibility(boolean allowed, String reason) {
        static CancellationEligibility ofAllowed() { return new CancellationEligibility(true, null); }
        static CancellationEligibility ofBlocked(String reason) { return new CancellationEligibility(false, reason); }
    }

    private CancellationEligibility cancellationEligibility(Prescription prescription, RelatedData data,
                                                             DispensingData dispensing) {
        if (prescription.getStatus() != PrescriptionStatus.ISSUED) {
            return CancellationEligibility.ofBlocked("Only an issued prescription can be cancelled.");
        }
        if (dispensing.status() == DispensingStatus.DISPENSED) {
            return CancellationEligibility.ofBlocked("A dispensed prescription cannot be cancelled.");
        }
        if (data.consultation().getStatus() == com.medisync.consultation.entity.ConsultationStatus.COMPLETED) {
            return CancellationEligibility.ofBlocked(
                    "This prescription cannot be cancelled after the consultation has been completed.");
        }
        if (prescription.getDoctorFeeStatus() == com.medisync.prescription.entity.DoctorFeeStatus.CONFIRMED) {
            return CancellationEligibility.ofBlocked(
                    "This prescription cannot be cancelled after payment has been confirmed.");
        }
        return CancellationEligibility.ofAllowed();
    }
}
