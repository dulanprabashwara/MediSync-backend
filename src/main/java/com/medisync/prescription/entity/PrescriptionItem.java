package com.medisync.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    private UUID id;

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    @Column(nullable = false)
    private int position;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(length = 100)
    private String strength;

    @Column(nullable = false, length = 200)
    private String dosage;

    @Column(nullable = false, length = 200)
    private String frequency;

    @Column(nullable = false, length = 200)
    private String duration;

    @Column(length = 100)
    private String quantity;

    @Column(name = "medicine_form", length = 100)
    private String medicineForm;

    @Column(length = 100)
    private String route;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PrescriptionItem() {
    }

    public PrescriptionItem(UUID prescriptionId, int position, String medicineName, String strength,
                            String dosage, String frequency, String duration, String quantity,
                            String medicineForm, String route, String instructions) {
        this.id = UUID.randomUUID();
        this.prescriptionId = prescriptionId;
        this.position = position;
        this.medicineName = medicineName;
        this.strength = strength;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.quantity = quantity;
        this.medicineForm = medicineForm;
        this.route = route;
        this.instructions = instructions;
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

    public UUID getId() { return id; }
    public UUID getPrescriptionId() { return prescriptionId; }
    public int getPosition() { return position; }
    public String getMedicineName() { return medicineName; }
    public String getStrength() { return strength; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public String getDuration() { return duration; }
    public String getQuantity() { return quantity; }
    public String getMedicineForm() { return medicineForm; }
    public String getRoute() { return route; }
    public String getInstructions() { return instructions; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
