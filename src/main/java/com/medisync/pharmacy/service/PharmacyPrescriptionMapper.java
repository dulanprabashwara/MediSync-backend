package com.medisync.pharmacy.service;

import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.hospital.entity.Hospital;
import com.medisync.hospital.repository.HospitalRepository;
import com.medisync.pharmacy.dto.DispensationHistoryDetail;
import com.medisync.pharmacy.dto.DispensationHistorySummary;
import com.medisync.pharmacy.dto.DispensingPrescriptionItemResponse;
import com.medisync.pharmacy.dto.PharmacyPrescriptionVerificationResponse;
import com.medisync.pharmacy.dto.PharmacyVerificationStatus;
import com.medisync.pharmacy.entity.PrescriptionDispensation;
import com.medisync.prescription.entity.Prescription;
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

import java.util.List;

@Component
public class PharmacyPrescriptionMapper {

    private final PrescriptionItemRepository itemRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AppUserRepository appUserRepository;
    private final HospitalRepository hospitalRepository;
    private final SpecializationRepository specializationRepository;

    public PharmacyPrescriptionMapper(PrescriptionItemRepository itemRepository,
                                      DoctorProfileRepository doctorProfileRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      AppUserRepository appUserRepository,
                                      HospitalRepository hospitalRepository,
                                      SpecializationRepository specializationRepository) {
        this.itemRepository = itemRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.appUserRepository = appUserRepository;
        this.hospitalRepository = hospitalRepository;
        this.specializationRepository = specializationRepository;
    }

    public PharmacyPrescriptionVerificationResponse verified(Prescription prescription) {
        RelatedData data = relatedData(prescription);
        return new PharmacyPrescriptionVerificationResponse(PharmacyVerificationStatus.VERIFIED,
                "Prescription verified", true, fullName(data.patientUser()), "Dr. " + fullName(data.doctorUser()),
                data.doctor().getMedicalRegistrationNumber(), data.specialization().getName(),
                data.hospital().getName(), prescription.getIssuedAt(), prescription.getValidUntil(),
                items(prescription), prescription.getGeneralInstructions(), null, null);
    }

    public DispensationHistorySummary historySummary(PrescriptionDispensation dispensation,
                                                     Prescription prescription) {
        RelatedData data = relatedData(prescription);
        return new DispensationHistorySummary(dispensation.getId(), dispensation.getDispensedAt(),
                fullName(data.patientUser()), "Dr. " + fullName(data.doctorUser()),
                dispensation.getPharmacyNameSnapshot(), itemRepository.countByPrescriptionId(prescription.getId()));
    }

    public DispensationHistoryDetail historyDetail(PrescriptionDispensation dispensation,
                                                   Prescription prescription) {
        RelatedData data = relatedData(prescription);
        return new DispensationHistoryDetail(dispensation.getId(), dispensation.getDispensedAt(),
                fullName(data.patientUser()), "Dr. " + fullName(data.doctorUser()),
                dispensation.getPharmacyNameSnapshot(), dispensation.getPharmacistRegistrationSnapshot(),
                dispensation.getDispensingNote(), prescription.getIssuedAt(), prescription.getValidUntil(),
                items(prescription), prescription.getGeneralInstructions());
    }

    private List<DispensingPrescriptionItemResponse> items(Prescription prescription) {
        return itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId()).stream()
                .map(DispensingPrescriptionItemResponse::from).toList();
    }

    private RelatedData relatedData(Prescription prescription) {
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
        Specialization specialization = specializationRepository.findById(doctor.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
        if (!doctor.getId().equals(prescription.getDoctorId()) || !patient.getId().equals(prescription.getPatientId())) {
            throw new ResourceConflictException("Prescription professional ownership is inconsistent");
        }
        return new RelatedData(doctor, patient, doctorUser, patientUser, hospital, specialization);
    }

    private String fullName(AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private record RelatedData(DoctorProfile doctor, PatientProfile patient, AppUser doctorUser,
                               AppUser patientUser, Hospital hospital, Specialization specialization) {
    }
}
