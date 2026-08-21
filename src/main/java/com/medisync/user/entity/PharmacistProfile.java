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
@Table(name = "pharmacist_profiles")
public class PharmacistProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "professional_registration_number", length = 100, unique = true)
    private String professionalRegistrationNumber;

    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;

    @Column(name = "pharmacy_registration_number", length = 100)
    private String pharmacyRegistrationNumber;

    @Column(name = "pharmacy_address", length = 500)
    private String pharmacyAddress;

    @Column(length = 500)
    private String qualifications;

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

    protected PharmacistProfile() {
    }

    public PharmacistProfile(UUID userId) {
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

    public void updateProfessionalProfile(String professionalRegistrationNumber, String pharmacyName,
                                          String pharmacyRegistrationNumber, String pharmacyAddress,
                                          String qualifications) {
        this.professionalRegistrationNumber = professionalRegistrationNumber;
        this.pharmacyName = pharmacyName;
        this.pharmacyRegistrationNumber = pharmacyRegistrationNumber;
        this.pharmacyAddress = pharmacyAddress;
        this.qualifications = qualifications;
    }

    public void submitForVerification() {
        verificationStatus = VerificationStatus.PENDING;
        verificationRejectionReason = null;
        verifiedBy = null;
        verifiedAt = null;
        submittedForVerificationAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void verify(UUID adminUserId) {
        verificationStatus = VerificationStatus.VERIFIED;
        verificationRejectionReason = null;
        verifiedBy = adminUserId;
        verifiedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void reject(String reason) {
        verificationStatus = VerificationStatus.REJECTED;
        verificationRejectionReason = reason;
        verifiedBy = null;
        verifiedAt = null;
    }

    public boolean isComplete() {
        return professionalRegistrationNumber != null
                && pharmacyName != null
                && pharmacyAddress != null;
    }

    public boolean isAwaitingReview() {
        return verificationStatus == VerificationStatus.PENDING && submittedForVerificationAt != null;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getProfessionalRegistrationNumber() { return professionalRegistrationNumber; }
    public String getPharmacyName() { return pharmacyName; }
    public String getPharmacyRegistrationNumber() { return pharmacyRegistrationNumber; }
    public String getPharmacyAddress() { return pharmacyAddress; }
    public String getQualifications() { return qualifications; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getVerificationRejectionReason() { return verificationRejectionReason; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public OffsetDateTime getSubmittedForVerificationAt() { return submittedForVerificationAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
