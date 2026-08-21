package com.medisync.pharmacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "prescription_dispensations")
public class PrescriptionDispensation {

    @Id
    private UUID id;

    @Column(name = "prescription_id", nullable = false, unique = true)
    private UUID prescriptionId;

    @Column(name = "pharmacist_id", nullable = false)
    private UUID pharmacistId;

    @Column(name = "dispensed_at", nullable = false)
    private OffsetDateTime dispensedAt;

    @Column(name = "pharmacy_name_snapshot", nullable = false, length = 200)
    private String pharmacyNameSnapshot;

    @Column(name = "pharmacist_registration_snapshot", nullable = false, length = 100)
    private String pharmacistRegistrationSnapshot;

    @Column(name = "dispensing_note", columnDefinition = "TEXT")
    private String dispensingNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected PrescriptionDispensation() {
    }

    public PrescriptionDispensation(UUID prescriptionId, UUID pharmacistId, OffsetDateTime dispensedAt,
                                    String pharmacyNameSnapshot, String pharmacistRegistrationSnapshot,
                                    String dispensingNote) {
        id = UUID.randomUUID();
        this.prescriptionId = prescriptionId;
        this.pharmacistId = pharmacistId;
        this.dispensedAt = dispensedAt;
        this.pharmacyNameSnapshot = pharmacyNameSnapshot;
        this.pharmacistRegistrationSnapshot = pharmacistRegistrationSnapshot;
        this.dispensingNote = dispensingNote;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getPrescriptionId() { return prescriptionId; }
    public UUID getPharmacistId() { return pharmacistId; }
    public OffsetDateTime getDispensedAt() { return dispensedAt; }
    public String getPharmacyNameSnapshot() { return pharmacyNameSnapshot; }
    public String getPharmacistRegistrationSnapshot() { return pharmacistRegistrationSnapshot; }
    public String getDispensingNote() { return dispensingNote; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
