package com.medisync.prescription.service;

import com.medisync.exception.ResourceNotFoundException;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PrescriptionAccessService {

    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionAccessService(CurrentUserService currentUserService,
                                     DoctorProfileRepository doctorProfileRepository,
                                     PatientProfileRepository patientProfileRepository,
                                     PrescriptionRepository prescriptionRepository) {
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    public DoctorProfile requireDoctor(Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, UserRole.DOCTOR, AccountStatus.ACTIVE);
        DoctorProfile doctor = doctorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccessDeniedException("Only verified doctors can manage prescriptions");
        }
        return doctor;
    }

    public PatientProfile requirePatient(Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, UserRole.PATIENT, AccountStatus.ACTIVE);
        return patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    public DoctorPrescriptionAccess requireDoctorPrescription(Jwt jwt, UUID prescriptionId, boolean forUpdate) {
        DoctorProfile doctor = requireDoctor(jwt);
        Prescription prescription = (forUpdate
                ? prescriptionRepository.findByIdForUpdate(prescriptionId)
                : prescriptionRepository.findById(prescriptionId))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        if (!doctor.getId().equals(prescription.getDoctorId())) {
            throw new AccessDeniedException("This prescription belongs to another doctor");
        }
        return new DoctorPrescriptionAccess(prescription, doctor);
    }

    public PatientPrescriptionAccess requirePatientPrescription(Jwt jwt, UUID prescriptionId) {
        return requirePatientPrescription(jwt, prescriptionId, false);
    }

    public PatientPrescriptionAccess requirePatientPrescription(Jwt jwt, UUID prescriptionId, boolean forUpdate) {
        PatientProfile patient = requirePatient(jwt);
        Prescription prescription = (forUpdate
                ? prescriptionRepository.findByIdForUpdate(prescriptionId)
                : prescriptionRepository.findById(prescriptionId))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        if (!patient.getId().equals(prescription.getPatientId())) {
            throw new AccessDeniedException("This prescription belongs to another patient");
        }
        if (prescription.getStatus() == PrescriptionStatus.DRAFT) {
            throw new ResourceNotFoundException("Prescription not found");
        }
        return new PatientPrescriptionAccess(prescription, patient);
    }

    public record DoctorPrescriptionAccess(Prescription prescription, DoctorProfile doctor) {}
    public record PatientPrescriptionAccess(Prescription prescription, PatientProfile patient) {}
}
