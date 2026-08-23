package com.medisync.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "medical_registration_number", length = 100, unique = true)
    private String medicalRegistrationNumber;

    // Retained for compatibility with the Phase 1 schema. Phase 2A uses specializationId.
    @Column(length = 150)
    private String specialization;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "specialization_id")
    private UUID specializationId;

    @Column(length = 500)
    private String qualifications;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(length = 2000)
    private String bio;

    @Column(name = "bank_account_holder", length = 200)
    private String bankAccountHolder;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private VerificationStatus verificationStatus;

    @Column(name = "verification_rejection_reason", length = 1000)
    private String verificationRejectionReason;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "submitted_for_verification_at")
    private OffsetDateTime submittedForVerificationAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DoctorProfile() {
    }

    public DoctorProfile(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateProfessionalProfile(String medicalRegistrationNumber, UUID hospitalId, UUID departmentId,
                                          UUID specializationId, String qualifications, Integer yearsOfExperience,
                                          String bio) {
        this.medicalRegistrationNumber = medicalRegistrationNumber;
        this.hospitalId = hospitalId;
        this.departmentId = departmentId;
        this.specializationId = specializationId;
        this.qualifications = qualifications;
        this.yearsOfExperience = yearsOfExperience;
        this.bio = bio;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updatePaymentDetails(String bankAccountHolder, String bankName, String bankBranch, String bankAccountNumber) {
        this.bankAccountHolder = bankAccountHolder;
        this.bankName = bankName;
        this.bankBranch = bankBranch;
        this.bankAccountNumber = bankAccountNumber;
    }

    public void anonymize() {
        this.bio = null;
    }

    public void submitForVerification() {
        this.verificationStatus = VerificationStatus.PENDING;
        this.verificationRejectionReason = null;
        this.verifiedBy = null;
        this.verifiedAt = null;
        this.submittedForVerificationAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void verify(UUID adminUserId) {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verificationRejectionReason = null;
        this.verifiedBy = adminUserId;
        this.verifiedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void reject(String reason) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verificationRejectionReason = reason;
        this.verifiedBy = null;
        this.verifiedAt = null;
    }

    public boolean isComplete() {
        return medicalRegistrationNumber != null
                && hospitalId != null
                && departmentId != null
                && specializationId != null
                && qualifications != null
                && yearsOfExperience != null
                && yearsOfExperience >= 0;
    }

    public boolean isAwaitingReview() {
        return verificationStatus == VerificationStatus.PENDING && submittedForVerificationAt != null;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getMedicalRegistrationNumber() { return medicalRegistrationNumber; }
    public UUID getHospitalId() { return hospitalId; }
    public UUID getDepartmentId() { return departmentId; }
    public UUID getSpecializationId() { return specializationId; }
    public String getQualifications() { return qualifications; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public String getBio() { return bio; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getVerificationRejectionReason() { return verificationRejectionReason; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public OffsetDateTime getSubmittedForVerificationAt() { return submittedForVerificationAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getBankAccountHolder() { return bankAccountHolder; }
    public String getBankName() { return bankName; }
    public String getBankBranch() { return bankBranch; }
    public String getBankAccountNumber() { return bankAccountNumber; }
}
